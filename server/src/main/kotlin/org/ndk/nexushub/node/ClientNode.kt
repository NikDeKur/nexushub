@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.node

import dev.nikdekur.ndkore.ext.isBlankOrEmpty
import dev.nikdekur.ndkore.interfaces.Snowflake
import kotlinx.coroutines.awaitAll
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.auth.account.Account
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.packet.*
import org.ndk.nexushub.packet.PacketError.Code
import org.ndk.nexushub.packet.PacketError.Level
import org.ndk.nexushub.packet.`in`.PacketBatchSaveData
import org.ndk.nexushub.packet.`in`.PacketLoadData
import org.ndk.nexushub.packet.`in`.PacketPong
import org.ndk.nexushub.packet.`in`.PacketRequestLeaderboard
import org.ndk.nexushub.packet.`in`.PacketRequestTopPosition
import org.ndk.nexushub.packet.`in`.PacketSaveData
import org.ndk.nexushub.packet.out.PacketLeaderboard
import org.ndk.nexushub.packet.out.PacketPing
import org.ndk.nexushub.packet.out.PacketRequestSync
import org.ndk.nexushub.packet.out.PacketStopSession
import org.ndk.nexushub.packet.out.PacketTopPosition
import org.ndk.nexushub.packet.out.PacketUserData
import org.ndk.nexushub.scope.Scope
import org.ndk.nexushub.scope.ScopesManager
import org.ndk.nexushub.session.SessionsManager
import org.ndk.nexushub.util.GsonSupport
import org.ndk.nexushub.util.NexusData

class ClientNode(
    val talker: Talker,
    override val id: String,
    val account: Account,
) : Snowflake<String>, Talker by talker {

    /**
     * Time when this node was authenticated
     */
    val createdAt = System.currentTimeMillis()

    suspend fun processAuthenticatedPacket(context: IncomingContext<out Packet>) {
        val packet = context.packet

        // Drop a packet if it's a response.
        // All responses are handled by code sending the request
        if (context.isResponse)
            return

        @Suppress("UNCHECKED_CAST")
        when (packet) {
            is PacketLoadData -> processLoadDataPacket(context as IncomingContext<PacketLoadData>)
            is PacketSaveData -> processSaveDataPacket(context as IncomingContext<PacketSaveData>)
            is PacketBatchSaveData -> processBatchSaveDataPacket(context as IncomingContext<PacketBatchSaveData>)
            is PacketRequestLeaderboard -> processRequestLeaderboardPacket(context as IncomingContext<PacketRequestLeaderboard>)
            is PacketRequestTopPosition -> processTopPositionPacket(context as IncomingContext<PacketRequestTopPosition>)
        }
    }

    inline fun hasScopeAccess(scopeId: String): Boolean {
        return account.allowedScopes.contains(scopeId)
    }


    suspend inline fun IncomingContext<out Packet>.accessScope(scopeId: String): Scope? {
        val has = hasScopeAccess(scopeId)
        if (!has) {
            this.respond(
                PacketError(
                    Level.ERROR,
                    Code.SCOPE_IS_NOT_ALLOWED,
                    "Scope '${scopeId}' is not allowed for your account"
                )
            )
            return null
        }
        return ScopesManager.getScope(scopeId)
    }

    suspend fun processLoadDataPacket(context: IncomingContext<out PacketLoadData>) {
        val packet = context.packet
        val scopeId = packet.scopeId
        val holderId = packet.holderId

        val scope = context.accessScope(scopeId) ?: return

        // If any session already exists, send a stop session packet to a client
        // This would notify the client that session is no longer active
        // And force it to also return actual data, which would be given to a new session


        logger.info("Sessions before: ${SessionsManager.nodeToSessions}")

        val session = SessionsManager.getExistingSession(scopeId, holderId)
        val createSession = session?.let {
            logger.info("Session already exists for $holderId in $scopeId.")
            val node = it.node
            if (node != this) {
                logger.info("Session is not for this node. Requesting sync and stopping session...")
                node.requestHolderSyncAndStopSession(scope, holderId)
                true
            } else {
                logger.info("Session is for this node. Don't worry about it.")
                false
            }
        } != false

        if (createSession) {
            logger.info("Starting session for $holderId in $scopeId")
            SessionsManager.startSession(this, scope, holderId)
        }

        logger.info("Current sessions: ${SessionsManager.nodeToSessions}")


        val data = scope.loadData(holderId)
        val dataStr = GsonSupport.dataToString(data)

        val dataPacket = PacketUserData(holderId, scopeId, dataStr)
        context.respond(dataPacket)
    }


    suspend fun processSaveDataPacket(context: IncomingContext<out PacketSaveData>) {
        val packet = context.packet
        val scopeId = packet.scopeId
        val holderId = packet.holderId
        val scope = context.accessScope(scopeId) ?: return

        val dataStr = packet.data

        if (dataStr.isBlankOrEmpty()) {
            context.respond(PacketError(Level.ERROR, Code.ERROR_IN_DATA, "Data is empty"))
            return
        }

        // If any session exists, and it's not this one, return error to a client
        val session = SessionsManager.getExistingSession(scopeId, holderId)
        if (session != null && session.node != this) {
            context.respond(PacketError(Level.ERROR, Code.SESSION_ALREADY_EXISTS, "Another session already exists for this holder. Only one session is allowed."))
            return
        }

        try {
            setData(scope, holderId, dataStr)
        } catch (_: Exception) {
            context.respond(
                PacketError(
                    Level.ERROR,
                    Code.ERROR_IN_DATA,
                    "Error while saving data in $scope for $holderId ($dataStr)"
                )
            )
            return
        }

        context.respond(PacketOk("Data saved"))
    }



    suspend fun processBatchSaveDataPacket(context: IncomingContext<PacketBatchSaveData>) {
        val packet = context.packet
        val scopeId = packet.scopeId
        val scope = context.accessScope(scopeId) ?: return

        val data = packet.data

        saveBatchDataSafe(scope, data)

        context.respond(PacketOk("Data saved"))
    }

    suspend fun saveBatchDataSafe(scope: Scope, holderToData: Map<String, String>) {
        holderToData.mapNotNull {
            val holderId = it.key
            val dataStr = it.value
            try {
                val existingSession = SessionsManager.getExistingSession(scope.id, holderId)
                if (existingSession != null && existingSession.node != this) {
                    logger.warn(
                        "Another session already exists for this holder. " +
                        "Only one session is allowed. Skipping data for ${scope.id}:$holderId. Requested by $this"
                    )
                    return@mapNotNull null
                }

                val data = GsonSupport.dataFromString(dataStr)
                if (data.isEmpty())
                    return@mapNotNull null

                scope.queueDataSet(holderId, data)
            } catch (_: Exception) {
                // Do nothing by now
                null
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

        SessionsManager.requestSync(scope)

        val leaderboard = scope.getLeaderboard(field, startFrom, limit)
        logger.info("Leaderboard: $leaderboard")

        val position = requestPosition?.let {
            try {
                scope.getTopPosition(it, field)
            } catch (_: NumberFormatException) {
                context.respondError(Code.FIELD_IS_NOT_NUMBER)
                return
            }
        }

        logger.info("Position: $position")

        context.respond(PacketLeaderboard(leaderboard, position))
        logger.info("Responded")
    }

    suspend fun processTopPositionPacket(context: IncomingContext<PacketRequestTopPosition>) {
        logger.info("Processing top position packet")
        val packet = context.packet
        val scopeId = packet.scopeId
        val holderId = packet.holderId
        val field = packet.field
        val scope = context.accessScope(scopeId) ?: return

        SessionsManager.requestSync(scope)

        val entry = try {
            scope.getTopPosition(holderId, field)
        } catch (_: NumberFormatException) {
            context.respondError(Code.FIELD_IS_NOT_NUMBER)
            return
        }

        context.respond(PacketTopPosition(entry))
    }


    /**
     * Set data to the scope
     *
     * Before setting data, it will deserialise the data string to [NexusData] and remove empty fields
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
        val data = GsonSupport.dataFromString(dataStr)

        scope.setDataSync(holderId, data)
    }


    suspend fun requestHolderSyncAndStopSession(scope: Scope, holderId: String) {
        val stopPacket = PacketStopSession(scope.id, holderId)

        sendPacket<Unit>(stopPacket) {
            receive<PacketSaveData> {
                if (packet.scopeId != scope.id) {
                    logger.warn("Received data for different scope: ${packet.scopeId} instead of ${scope.id}")
                    return@receive
                }

                if (packet.holderId != holderId) {
                    logger.warn("Received data for different holder: ${packet.holderId} instead of $holderId")
                    return@receive
                }

                val dataStr = packet.data
                try {
                    setData(scope, holderId, dataStr)
                } catch (_: Exception) {
                    this.respond(
                        PacketError(
                            Level.ERROR,
                            Code.ERROR_IN_DATA,
                            "Error while saving data in $scope for $holderId ($dataStr)"
                        )
                    )
                    return@receive
                }
            }

            receive<PacketOk> {
                // Nothing to save
            }

            receive<PacketError> {
                if (packet.code == Code.SESSION_NOT_FOUND) {
                    logger.warn("Session not found while syncing data with node $this for holder $holderId. Packet: $packet. Removing session...")
                    SessionsManager.stopSession(scope.id, holderId)
                } else {
                    logger.error("Error returned while syncing data: $packet")
                }
            }

            timeout(5000) {}

            exception {
                logger.error("Exception occurred while syncing data: $exception")
            }
        }.await()

        SessionsManager.stopSession(scope.id, holderId)
    }


    /**
     * Request data synchronisation for this node, making it respond with all data related to the scope
     *
     * @param scope scope to sync
     */
    suspend fun requestSync(scope: Scope) {
        val syncPacket = PacketRequestSync(scope.id)

        @Suppress("RemoveExplicitTypeArguments")
        sendPacket<Unit>(syncPacket) {
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

            timeout(5000) {
                logger.warn("Timeout while global syncing data for scope ${scope.id} with node $id")
            }

            exception {
                logger.warn("Exception while syncing data!", exception)
            }
        }.await()
    }


    suspend fun ping(): Boolean {
        val packet = PacketPing()
        return sendPacket<Boolean>(packet) {
            receive<PacketPong> { true }
            receive { false }
            timeout(5000) { false }
            exception {
                logger.warn("[$addressStr] Exception while pinging: $exception")
                false
            }
        }.await()
    }


    override fun toString(): String {
        return "ClientNode(id='$id', account=$account)"
    }


    override suspend fun close(code: Short, reason: String) {
        cleanUp()

        if (talker.isOpen)
            talker.close(code, reason)
    }

    /**
     * Clean up all resources where this node is used
     *
     * Unregister:
     * - [talker] from [TalkersManager]
     * - self from [NodesManager]
     * - all sessions from all [Scope] from [ScopesManager]
     */
    fun cleanUp() {
        TalkersManager.removeTalker(talker.addressHash)
        NodesManager.removeNode(this)
        SessionsManager.stopAllSessions(this)
    }
}