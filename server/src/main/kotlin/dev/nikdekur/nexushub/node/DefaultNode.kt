/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.node

import dev.nikdekur.ndkore.ext.isBlankOrEmpty
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.account.Account
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.packet.ErrorCode
import dev.nikdekur.nexushub.packet.ErrorLevel
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.PacketBatchSaveData
import dev.nikdekur.nexushub.packet.PacketError
import dev.nikdekur.nexushub.packet.PacketHeartbeat
import dev.nikdekur.nexushub.packet.PacketHeartbeatACK
import dev.nikdekur.nexushub.packet.PacketLeaderboard
import dev.nikdekur.nexushub.packet.PacketLoadData
import dev.nikdekur.nexushub.packet.PacketOk
import dev.nikdekur.nexushub.packet.PacketRequestLeaderboard
import dev.nikdekur.nexushub.packet.PacketRequestSync
import dev.nikdekur.nexushub.packet.PacketRequestTopPosition
import dev.nikdekur.nexushub.packet.PacketSaveData
import dev.nikdekur.nexushub.packet.PacketTopPosition
import dev.nikdekur.nexushub.packet.PacketUserData
import dev.nikdekur.nexushub.packet.respondError
import dev.nikdekur.nexushub.scope.Scope
import dev.nikdekur.nexushub.scope.ScopesService
import dev.nikdekur.nexushub.serial.SerialService
import dev.nikdekur.nexushub.service.NexusHubComponent
import dev.nikdekur.nexushub.session.SessionsService
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.util.CloseCode
import dev.nikdekur.nexushub.util.NexusData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


class DefaultNode(
    override val app: NexusHubServer,
    val talker: Talker,
    val account: Account,
    override val id: String,
) : Node, Talker by talker, NexusHubComponent {

    val storage: StorageService by inject()
    val serialService: SerialService by inject()
    val scopesService: ScopesService by inject()
    val sessionsService: SessionsService by inject()
    val nodesService: NodesService by inject()

    val logger = LoggerFactory.getLogger(javaClass)

    var latestPingTime: Long = 0

    /**
     * Time when this node was authenticated
     */
    val createdAt = System.currentTimeMillis()

    override suspend fun processPacket(context: IncomingContext<out Packet>) {
        val packet = context.packet

        @Suppress("UNCHECKED_CAST")
        when (packet) {
            is PacketLoadData -> processLoadDataPacket(context as IncomingContext<PacketLoadData>)
            is PacketSaveData -> processSaveDataPacket(context as IncomingContext<PacketSaveData>)
            is PacketBatchSaveData -> processBatchSaveDataPacket(context as IncomingContext<PacketBatchSaveData>)
            is PacketRequestLeaderboard -> processRequestLeaderboardPacket(context as IncomingContext<PacketRequestLeaderboard>)
            is PacketRequestTopPosition -> processTopPositionPacket(context as IncomingContext<PacketRequestTopPosition>)
            is PacketHeartbeat -> processHeartbeatPacket(context as IncomingContext<PacketHeartbeat>)
            else -> {
                logger.warn("Unknown packet: $packet")
            }
        }
    }

    suspend inline fun hasScopeAccess(scopeId: String): Boolean {
        return account.getScopes().contains(scopeId)
    }


    suspend inline fun IncomingContext<out Packet>.accessScope(scopeId: String): Scope? {
        val has = hasScopeAccess(scopeId)
        if (!has) {
            this.respond<Unit>(
                PacketError(
                    ErrorLevel.ERROR,
                    ErrorCode.SCOPE_IS_NOT_ALLOWED,
                    "Scope '${scopeId}' is not allowed for your account"
                )
            )
            return null
        }
        return scopesService.getScope(scopeId)
    }

    suspend fun processLoadDataPacket(context: IncomingContext<out PacketLoadData>) {
        val packet = context.packet
        val scopeId = packet.scopeId
        val holderId = packet.holderId

        val scope = context.accessScope(scopeId) ?: return

        // If any session already exists, send a stop session packet to a client
        // This would notify the client that session is no longer active
        // And force it to also return actual data, which would be given to a new session


        val session = sessionsService.getExistingSession(scopeId, holderId)
        val createSession = session?.let {
            val node = it.node
            if (node != this) {
                logger.info("Session already exists for $holderId in $scopeId. Denying request.")
                node.send<Unit>(
                    PacketError(
                        ErrorLevel.ERROR,
                        ErrorCode.SESSION_ALREADY_EXISTS,
                        "Another session already exists for this holder. Only one session is allowed."
                    )
                )
                true
            } else false
        } != false

        if (createSession) {
            logger.info("Starting session for $holderId in $scopeId")
            sessionsService.startSession(this, scope, holderId)
        }


        val data = scope.loadData(holderId)
        val dataStr = serialService.serialize(data)

        val dataPacket = PacketUserData(holderId, scopeId, dataStr)
        context.respond<Unit>(dataPacket)
    }


    suspend fun processSaveDataPacket(context: IncomingContext<out PacketSaveData>) {
        val packet = context.packet
        val scopeId = packet.scopeId
        val holderId = packet.holderId
        val scope = context.accessScope(scopeId) ?: return

        val dataStr = packet.data

        if (dataStr.isBlankOrEmpty()) {
            context.respond<Unit>(
                PacketError(
                    ErrorLevel.ERROR,
                    ErrorCode.ERROR_IN_DATA,
                    "Data is empty"
                )
            )
            return
        }

        // If any session exists, and it's not this one, return error to a client
        val session = sessionsService.getExistingSession(scopeId, holderId)
        if (session != null && session.node != this) {
            context.respond<Unit>(
                PacketError(
                    ErrorLevel.ERROR,
                    ErrorCode.SESSION_ALREADY_EXISTS,
                    "Another session already exists for this holder. Only one session is allowed."
                )
            )
            return
        }

        try {
            setData(scope, holderId, dataStr)
        } catch (_: Exception) {
            context.respond<Unit>(
                PacketError(
                    ErrorLevel.ERROR,
                    ErrorCode.ERROR_IN_DATA,
                    "Error while saving data in $scope for $holderId ($dataStr)"
                )
            )
            return
        }

        context.respond<Unit>(PacketOk("Data saved"))
    }


    suspend fun processBatchSaveDataPacket(context: IncomingContext<PacketBatchSaveData>) {
        val packet = context.packet
        val scopeId = packet.scopeId
        val scope = context.accessScope(scopeId) ?: return

        val data = packet.data

        saveBatchDataSafe(scope, data)

        context.respond<Unit>(PacketOk("Data saved"))
    }


    suspend fun saveBatchDataSafe(scope: Scope, holderToData: Map<String, String>) {
        holderToData.mapNotNull {
            val holderId = it.key
            val dataStr = it.value
            try {
                val existingSession = sessionsService.getExistingSession(scope.id, holderId)
                if (existingSession != null && existingSession.node != this) {
                    logger.warn(
                        "Another session already exists for this holder. " +
                                "Only one session is allowed. Skipping data for ${scope.id}:$holderId. Requested by $this"
                    )
                    return@mapNotNull null
                }

                val data = serialService.deserialize(dataStr)
                if (data.isEmpty())
                    return@mapNotNull null

                storage.scope.async {
                    scope.setData(holderId, data)
                }
            } catch (_: Exception) {
                // Do nothing by now
                null
            }
        }.awaitAll()
    }

    suspend inline fun SessionsService.requestSync(scope: Scope) {
        val nodes = getNodes(scope)

        nodes.map {
            nodesService.syncScope.async {
                it.requestScopeSync(scope)
            }
        }.awaitAll()
    }

    suspend fun processRequestLeaderboardPacket(context: IncomingContext<PacketRequestLeaderboard>) {
        logger.info("Processing leaderboard packet")
        val packet = context.packet
        val scopeId = packet.scopeId
        val field = packet.field
        val startFrom = packet.startFrom
        val limit = packet.limit
        val requestPosition: String? = packet.requestPosition.let {
            if (it.isBlankOrEmpty()) null else it
        }
        val scope = context.accessScope(scopeId) ?: return

        sessionsService.requestSync(scope)

        val leaderboard = scope.getLeaderboard(field, startFrom, limit)
        logger.info("Leaderboard: $leaderboard")

        val position = requestPosition?.let {
            try {
                scope.getTopPosition(it, field)
            } catch (_: NumberFormatException) {
                context.respondError(ErrorCode.FIELD_IS_NOT_NUMBER)
                return
            }
        }

        logger.info("Position: $position")

        val packetLeaderboard = PacketLeaderboard(startFrom, leaderboard, position)
        context.respond<Unit>(packetLeaderboard)
        logger.info("Responded with $packetLeaderboard")
    }

    suspend fun processTopPositionPacket(context: IncomingContext<PacketRequestTopPosition>) {
        logger.info("Processing top position packet")
        val packet = context.packet
        val scopeId = packet.scopeId
        val holderId = packet.holderId
        val field = packet.field
        val scope = context.accessScope(scopeId) ?: return

        sessionsService.requestSync(scope)

        val entry = try {
            scope.getTopPosition(holderId, field)
        } catch (_: NumberFormatException) {
            context.respondError(ErrorCode.FIELD_IS_NOT_NUMBER)
            return
        }

        context.respond<Unit>(PacketTopPosition(entry))
    }


    suspend fun processHeartbeatPacket(context: IncomingContext<PacketHeartbeat>) {
        latestPingTime = System.currentTimeMillis()
        context.respond<Unit>(PacketHeartbeatACK())
    }

    /**
     * Set data to the scope
     *
     * Before setting data, it will deserialize the data string to [NexusData] and remove empty fields
     *
     * "Set data" means that the data will be saved to the scope cache, but not to the database
     *
     * During executing can throw an exception if the data is not valid
     *
     * @param scope scope to set data
     * @param holderId holder id
     * @param dataStr data string to set
     */
    suspend fun setData(scope: Scope, holderId: String, dataStr: String) {
        val data = serialService.deserialize(dataStr)

        scope.setData(holderId, data)
    }


    /**
     * Request data synchronization for this node, making it respond with all data related to the scope
     *
     * @param scope scope to sync
     */
    override suspend fun requestScopeSync(scope: Scope) {
        val syncPacket = PacketRequestSync(scope.id)

        @Suppress("RemoveExplicitTypeArguments")
        send(syncPacket) {
            receive<PacketBatchSaveData> {
                if (packet.scopeId != scope.id) {
                    logger.warn("Received data for different scope: ${packet.scopeId} instead of ${scope.id}")
                    return@receive
                }
                saveBatchDataSafe(scope, packet.data)
            }

            receive<PacketOk> {
                // Nothing to save
            }

            receive {
                logger.warn("Unexpected behaviour while global syncing data: ${this.packet}")
            }

            timeout(5.seconds) {
                logger.warn("Timeout while global syncing data for scope ${scope.id} with node $id")
            }

            exception {
                logger.warn("Exception while syncing data!", exception)
            }
        }.await()
    }


    override fun isAlive(deadInterval: Duration): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - maxOf(latestPingTime, createdAt)

        return timeDiff < deadInterval.inWholeMilliseconds
    }


    override fun toString(): String {
        return "ClientNode(id='$id', account=$account)"
    }


    override suspend fun close(code: CloseCode, comment: String) {
        nodesService.removeNode(this)
        sessionsService.stopAllSessions(this)
        talker.close(code, comment)
    }
}