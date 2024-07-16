package dev.nikdekur.nexushub.service

import dev.nikdekur.ndkore.ext.*
import dev.nikdekur.nexushub.NexusHub
import dev.nikdekur.nexushub.connection.prepareBatchSaveData
import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.packet.PacketOk
import dev.nikdekur.nexushub.packet.`in`.PacketBatchSaveData
import dev.nikdekur.nexushub.packet.`in`.PacketEndSession
import dev.nikdekur.nexushub.packet.`in`.PacketRequestLeaderboard
import dev.nikdekur.nexushub.packet.`in`.PacketRequestTopPosition
import dev.nikdekur.nexushub.packet.out.PacketLeaderboard
import dev.nikdekur.nexushub.packet.out.PacketTopPosition
import dev.nikdekur.nexushub.serialization.DataSerializer
import dev.nikdekur.nexushub.sesion.Session
import dev.nikdekur.nexushub.sesion.SessionImpl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractNexusService<H, S>(
    override val hub: NexusHub
) : NexusService<H, S> {

    override val logger: Logger by lazy {
        LoggerFactory.getLogger("NexusHub-$scope")
    }

    open val sessionsLimit: Long = -1
    abstract override val serializer: DataSerializer<H, S>

    val sessionsCache = ConcurrentHashMap<String, Session<H, S>>()

    override fun createSession(holder: H): Session<H, S> {
        return SessionImpl(this, holder)
    }

    override fun removeSession(session: Session<H, S>) {
        session.state = Session.State.INACTIVE
        sessionsCache.remove(session.id)
    }

    override val sessions: Collection<Session<H, S>>
        get() = sessionsCache.values

    override var isRunning: Boolean = false

    override fun start() {
        isRunning = true
    }

    override suspend fun stop() {
        logger.info { "Stopping service $scope" }

        isRunning = false

        sessionsCache.clear {
            value.stop()
        }
    }

    override fun getExistingSession(holderId: String): Session<H, S>? {
        return sessionsCache[holderId]
    }


    override suspend fun stopSession(session: Session<H, S>) {
        if (session.state.isInactive) return

        val dataStr = session.serializeData()

        session.state = Session.State.STOPPING

        val packet = PacketEndSession(scope, session.id, dataStr)

        @Suppress("RemoveExplicitTypeArguments")
        hub.gateway.sendPacket<Unit>(packet) {
            receive<PacketOk> {}

            timeout(5000) {
                hub.logger.warn("Timeout while stopping session.")
            }

            receive {
                hub.logger.warn("Error while stopping session: $packet")
            }
        }.await()

        removeSession(session)
    }

    override suspend fun startSession(holder: H): Session<H, S> {
        val id = getId(holder)
        getExistingSession(id)?.let { stopSession(it) }
        val session = createSession(holder)
        sessionsCache.put(id, session)
        session.loadData()

        return session
    }


    override suspend fun saveAllSessions() {
        val data = prepareBatchSaveData()
        if (data.isEmpty()) return
        val packet = PacketBatchSaveData(scope, data)
        hub.gateway.sendPacket<Unit>(packet) {
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

        return hub.gateway.sendPacket(packet) {
            throwOnTimeout(5000)

            receive<PacketLeaderboard> {
                this.packet.leaderboard
            }

            receive {
                error("Unexpected behaviour while loading leaderboard in scope '$scope', for field '$field', with limit '$limit': $packet")
            }
        }.await()
    }

    override suspend fun getLeaderboardPosition(field: String, holderId: String): LeaderboardEntry? {
        val packet = PacketRequestTopPosition(scope, holderId, field)

        @Suppress("RemoveExplicitTypeArguments")
        return hub.gateway.sendPacket<LeaderboardEntry?>(packet) {
            throwOnTimeout(5000)

            receive<PacketTopPosition> {
                this.packet.entry
            }

            receive {
                logger.error(
                    "Unexpected behaviour while loading top position in scope '$scope', " +
                            "for field '$field': $packet")
                null
            }
        }.await()
    }

    override suspend fun getLeaderboardAndPosition(field: String, startFrom: Int, limit: Int, holderId: String): Pair<Leaderboard, LeaderboardEntry?> {
        check(holderId.isNotEmpty()) { "positionOf cannot be empty" }
        val packet = PacketRequestLeaderboard(scope, field, startFrom, limit, holderId)

        return hub.gateway.sendPacket(packet) {
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