/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.service

import dev.nikdekur.ndkore.ext.*
import dev.nikdekur.nexushub.NexusHub
import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.event.NetworkEvent
import dev.nikdekur.nexushub.packet.PacketError
import dev.nikdekur.nexushub.packet.PacketError.Level
import dev.nikdekur.nexushub.packet.PacketOk
import dev.nikdekur.nexushub.packet.`in`.PacketBatchSaveData
import dev.nikdekur.nexushub.packet.`in`.PacketEndSession
import dev.nikdekur.nexushub.packet.`in`.PacketRequestLeaderboard
import dev.nikdekur.nexushub.packet.`in`.PacketRequestTopPosition
import dev.nikdekur.nexushub.packet.`in`.PacketSaveData
import dev.nikdekur.nexushub.packet.out.PacketLeaderboard
import dev.nikdekur.nexushub.packet.out.PacketTopPosition
import dev.nikdekur.nexushub.scope.ScopeData
import dev.nikdekur.nexushub.serialization.DataSerializer
import dev.nikdekur.nexushub.sesion.Session
import dev.nikdekur.nexushub.sesion.SessionImpl
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractNexusService<H, S : ScopeData<S>>(
    override val hub: NexusHub
) : NexusService<H, S> {

    val logger = LoggerFactory.getLogger("NexusHub-$scope")

    open val sessionsLimit: Long = -1
    abstract override val serializer: DataSerializer<H, S>

    val sessionsCache = ConcurrentHashMap<String, Session<H, S>>()

    override fun createSession(holder: H): Session<H, S> {
        return SessionImpl(this, holder)
    }

    open fun removeSession(session: Session<H, S>) {
        session.state = Session.State.INACTIVE
        sessionsCache.remove(session.id)
    }

    override val sessions: Collection<Session<H, S>>
        get() = sessionsCache.values

    override var isActive: Boolean = false

    override fun start() {
        isActive = true
    }

    override suspend fun stop() {
        logger.info { "Stopping service $scope" }

        isActive = false

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
        require(holderId.isNotEmpty()) { "positionOf cannot be empty" }
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

    override suspend fun onEvent(event: NetworkEvent.ScopeEvent) {
        when (event) {
            is NetworkEvent.StopSession -> processStopSession(event)
            is NetworkEvent.Sync -> processSyncData(event)
            else -> {
                // ignore
            }
        }
    }

    suspend fun processStopSession(event: NetworkEvent.StopSession) {
        val scope = event.scopeId
        val holderId = event.holderId

        val session = getExistingSession(holderId)
        if (session == null) {
            event.context.respond<String>(
                PacketError(
                    Level.ERROR,
                    PacketError.Code.SESSION_NOT_FOUND,
                    "No session found"
                )
            )
            return
        }

        if (!session.hasDataUpdated()) {
            event.respond(PacketOk("No data to save"))
            return
        }

        val data = session.serializeData()
        val savePacket = PacketSaveData(scope, holderId, data)

        removeSession(session)

        event.respond(savePacket)
    }



    suspend fun processSyncData(event: NetworkEvent.Sync) {
        val scopeId = event.scopeId

        logger.debug("[$scopeId] Processing sync data")

        suspend fun ok() {
            logger.debug("[$scopeId] No data to save")
            event.respond(PacketOk("No data to save"))
        }

        val batchMap = prepareBatchSaveData()
        if (batchMap.isEmpty())
            return ok()


        val batchPacket = PacketBatchSaveData(scopeId, batchMap)
        logger.debug("[$scopeId] Sending batch save data")
        event.respond(batchPacket)
    }

    suspend fun prepareBatchSaveData(): Map<String, String> {
        val sessions = sessions
        if (!isActive || sessions.isEmpty())
            return emptyMap()

        val batchMap = HashMap<String, String>()

        sessions.forEach {
            if (!it.hasDataUpdated()) return@forEach

            val dataStr = it.serializeData()
            batchMap[it.id] = dataStr
        }

        return batchMap
    }

}