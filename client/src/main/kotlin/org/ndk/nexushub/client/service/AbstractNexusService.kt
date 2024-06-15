package org.ndk.nexushub.client.service

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import dev.nikdekur.ndkore.ext.*
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.sesion.Session
import org.ndk.nexushub.data.Leaderboard
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.network.GsonSupport
import org.ndk.nexushub.network.NexusData
import org.ndk.nexushub.packet.PacketLeaderboard
import org.ndk.nexushub.packet.PacketLoadData
import org.ndk.nexushub.packet.PacketRequestLeaderboard
import org.ndk.nexushub.packet.PacketUserData
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractNexusService<H : Any, S : Session<H, S>>(
    override val hub: NexusHub
) : NexusService<H, S> {

    override val logger: Logger by lazy {
        LoggerFactory.getLogger("NexusHub-$scope")
    }

    open val sessionsLimit: Long = -1
    open val saveParallelism: Int = 32

    val sessionsCache: Cache<String, S> by lazy {
        preBuildCache().build()
    }

    open fun preBuildCache(): CacheBuilder<String, S> {
        return CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .apply {
                if (sessionsLimit > 0) {
                    maximumSize(sessionsLimit)
                }
            }
            .removalListener {
                if (!isRunning) return@removalListener
                hub.blockingScope.launch {
                    val session = it.value!!
                    if (!session.isActive) return@launch
                    it.value!!.stop()
                }
            }
    }

    override val sessions: Collection<S>
        get() = sessionsCache.asMap().values

    override var isRunning: Boolean = false

    override fun start() {
        isRunning = true
    }

    override suspend fun stop() {
        logger.info { "Stopping service $scope" }
        isRunning = false
        val sessions = sessionsCache.asMap().values
        hub.blockingScope.parallel(saveParallelism, sessions) {
            it.stop()
        }.awaitAll()

        sessionsCache.invalidateAll()
    }

    override fun getExistingSession(holderId: String): S? {
        return sessionsCache.getIfPresent(holderId)
    }


    override suspend fun stopSession(holderId: String): S? {
        val session = getExistingSession(holderId)
        if (session != null) {
            // Removal Listener will execute stopSessionInternal
            sessionsCache.invalidate(holderId)
        }
        return session
    }

    override suspend fun startSession(holder: H): S {
        stopSession(getId(holder))
        val session = createSession(holder)
        sessionsCache.put(getId(holder), session)
        session.loadData()
        return session
    }



    override suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): Leaderboard {
        val packet = PacketRequestLeaderboard(scope, field, startFrom, limit, "")

        return hub.connection.talker!!.sendPacket(packet) {
            throwOnTimeout(5000)

            receive<PacketLeaderboard> {
                this.packet.leaderboard
            }

            receive {
                error { "Unexpected behaviour while loading leaderboard in scope '$scope', for field '$field', with limit '$limit': $packet" }
            }
        }.await()
    }

    override suspend fun getLeaderboardAndPosition(field: String, startFrom: Int, limit: Int, holderId: String): Pair<Leaderboard, LeaderboardEntry?> {
        check(holderId.isNotEmpty()) { "positionOf cannot be empty" }
        val packet = PacketRequestLeaderboard(scope, field, startFrom, limit, holderId)

        return hub.connection.talker!!.sendPacket(packet) {
            throwOnTimeout(5000)

            receive<PacketLeaderboard> {
                this.packet
            }

            receive {
                error { "Unexpected behaviour while loading leaderboard and position in scope '$scope', for field '$field', with limit '$limit' and position for holder '$holderId': $packet" }
            }
        }
            .await()
            .let { it.leaderboard to it.requestPosition }
    }


    override suspend fun getActualData(holderId: String): NexusData {
        val packet = PacketLoadData(scope, holderId)

        val dataStr = hub.connection.talker!!.sendPacket(packet) {
            throwOnTimeout(5000)

            receive<PacketUserData> {
                this.packet.data
            }

            receive {
                error { "Unexpected behaviour while 'getActualData' for '$holderId': $packet" }
            }
        }.await()

        val data = GsonSupport.dataFromString(dataStr)

        return data
    }
}