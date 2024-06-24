package org.ndk.nexushub.client.service

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.nikdekur.ndkore.ext.*
import kotlinx.coroutines.launch
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.serialization.DataSerializer
import org.ndk.nexushub.client.sesion.Session
import org.ndk.nexushub.client.sesion.SessionImpl
import org.ndk.nexushub.data.Leaderboard
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.packet.PacketOk
import org.ndk.nexushub.packet.`in`.PacketBatchSaveData
import org.ndk.nexushub.packet.`in`.PacketEndSession
import org.ndk.nexushub.packet.`in`.PacketRequestLeaderboard
import org.ndk.nexushub.packet.out.PacketLeaderboard
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractNexusService<H, S>(
    override val hub: NexusHub
) : NexusService<H, S> {

    override val logger: Logger by lazy {
        LoggerFactory.getLogger("NexusHub-$scope")
    }

    open val sessionsLimit: Long = -1
    abstract override val serializer: DataSerializer<H, S>

    val sessionsCache: Cache<String, Session<H, S>> by lazy {
        preBuildCache().build()
    }

    open fun preBuildCache(): CacheBuilder<String, Session<H, S>> {
        return CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .apply {
                if (sessionsLimit > 0) {
                    maximumSize(sessionsLimit)
                }
            }
            .removalListener<String, Session<H, S>> {
                logger.info { "Removing session ${it.key} from cache." }
                if (it.value!!.state.isInactive) return@removalListener
                hub.scheduler.launch {
                    stopSession(it.key!!)
                }
            }
    }

    override fun createSession(holder: H): Session<H, S> {
        return SessionImpl(this, holder)
    }

    override fun removeSession(session: Session<H, S>) {
        session.state = Session.State.INACTIVE
        sessionsCache.invalidate(session.id)
    }

    override val sessions: Collection<Session<H, S>>
        get() = sessionsCache.asMap().values

    override var isRunning: Boolean = false

    override fun start() {
        isRunning = true
    }

    override suspend fun stop() {
        logger.info { "Stopping service $scope" }

        isRunning = false

        sessionsCache.asMap().forEach {
            it.value.stop()
        }

        sessionsCache.invalidateAll()
    }

    override fun getExistingSession(holderId: String): Session<H, S>? {
        return sessionsCache.getIfPresent(holderId)
    }


    override suspend fun stopSession(holderId: String): Session<H, S>? {
        // Function is suspending due to sessionsCache.invalidate call saveData
        val session = getExistingSession(holderId) ?: return null

        if (session.state.isInactive) return session

        val dataStr = session.serializeData()

        session.state = Session.State.STOPPING

        val packet = PacketEndSession(scope, session.id, dataStr)

        @Suppress("RemoveExplicitTypeArguments")
        hub.connection.sendPacket<Unit>(packet) {
            receive<PacketOk> {}

            timeout(5000) {
                hub.logger.warn("Timeout while stopping session.")
            }

            receive {
                hub.logger.warn("Error while stopping session: $packet")
            }
        }.await()

        removeSession(session)

        return session
    }

    override suspend fun startSession(holder: H): Session<H, S> {
        val id = getId(holder)
        stopSession(id)
        val session = createSession(holder)
        sessionsCache.put(id, session)
        session.loadData()

        return session
    }


    override suspend fun saveAllSessions() {
        val data = hub.connection.prepareBatchSaveData(this)
        if (data.isEmpty()) return
        val packet = PacketBatchSaveData(scope, data)
        hub.connection.sendPacket<Unit>(packet) {
            receive<PacketOk> {}

            timeout(5000) {
                logger.warn("Timeout while saving all sessions.")
            }

            receive {
                logger.warn("Error while saving all sessions: $packet")
            }
        }.await()
    }



    override suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): Leaderboard {
        val packet = PacketRequestLeaderboard(scope, field, startFrom, limit, "")

        return hub.connection.sendPacket(packet) {
            throwOnTimeout(5000)

            receive<PacketLeaderboard> {
                this.packet.leaderboard
            }

            receive {
                error("Unexpected behaviour while loading leaderboard in scope '$scope', for field '$field', with limit '$limit': $packet")
            }
        }.await()
    }

    override suspend fun getLeaderboardAndPosition(field: String, startFrom: Int, limit: Int, holderId: String): Pair<Leaderboard, LeaderboardEntry?> {
        check(holderId.isNotEmpty()) { "positionOf cannot be empty" }
        val packet = PacketRequestLeaderboard(scope, field, startFrom, limit, holderId)

        return hub.connection.sendPacket(packet) {
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
}