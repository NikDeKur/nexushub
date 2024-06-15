@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.node

import dev.nikdekur.ndkore.ext.isBlankOrEmpty
import dev.nikdekur.ndkore.interfaces.Snowflake
import kotlinx.coroutines.awaitAll
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.auth.account.Account
import org.ndk.nexushub.network.GsonSupport
import org.ndk.nexushub.network.NexusData
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.packet.*
import org.ndk.nexushub.packet.PacketError.Code
import org.ndk.nexushub.packet.PacketError.Level
import org.ndk.nexushub.scope.Scope
import org.ndk.nexushub.scope.ScopesManager
import org.ndk.nexushub.session.SessionsManager
import org.ndk.nexushub.session.SessionsManager.stopSession

class ClientNode(
    val talker: Talker,
    override val id: String,
    val account: Account,
) : Snowflake<String>, Talker by talker {

    var isAlive = true

    suspend fun processAuthenticatedPacket(context: IncomingContext<out Packet>) {
        val packet = context.packet

        // Drop a packet if it's a response.
        // All responses are handled by code sending the request
        if (context.isResponse)
            return

        @Suppress("UNCHECKED_CAST")
        when (packet) {
            is PacketCreateSession -> processCreateSessionPacket(context as IncomingContext<PacketCreateSession>)
            is PacketLoadData -> processLoadDataPacket(context as IncomingContext<PacketLoadData>)
            is PacketStopSession -> processStopSessionPacket(context as IncomingContext<PacketStopSession>)
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

    suspend fun processCreateSessionPacket(context: IncomingContext<PacketCreateSession>) {
        val packet = context.packet
        val scopeId  = packet.scopeId
        val holderId = packet.holderId

        val scope = context.accessScope(scopeId) ?: return
        val sessions = SessionsManager.getExistingSession(scope.id, holderId)
        if (sessions != null) {
            context.respond(
                PacketError(
                    Level.ERROR,
                    Code.SESSION_ALREADY_EXISTS,
                    "Creating a few write sessions is not allowed"
                )
            )
            return
        }

        val data = loadData(scope, holderId)
        val dataStr = GsonSupport.dataToString(data)

        SessionsManager.startSession(this, scope, holderId)

        val dataPacket = PacketUserData(holderId, scopeId, dataStr)
        context.respond(dataPacket)
    }

    suspend fun processStopSessionPacket(context: IncomingContext<PacketStopSession>) {
        val packet = context.packet
        val scopeId = packet.scopeId
        val holderId = packet.holderId

        val scope = context.accessScope(scopeId) ?: return

        val session = SessionsManager.getExistingSession(scopeId, holderId)
        if (session == null) {
            context.respond(
                PacketError(
                    Level.ERROR,
                    Code.SESSION_NOT_FOUND,
                    "Session not found"
                )
            )
            return
        }

        // Save data and stop session
        try {
            setData(scope, holderId, packet.data)
        } catch (e: Exception) {
            context.respond(
                PacketError(
                    Level.ERROR,
                    Code.ERROR_IN_DATA,
                    "Error while converting data from JSON"
                )
            )
            return
        }

        context.respond(PacketOk("Session stopped"))
    }

    suspend fun processLoadDataPacket(context: IncomingContext<out PacketLoadData>) {
        val packet = context.packet
        val scopeId = packet.scopeId
        val holderId = packet.holderId

        val scope = context.accessScope(scopeId) ?: return

        val data = loadData(scope, holderId)
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
                val data = GsonSupport.dataFromString(dataStr)
                data.apply {

                }
                if (data.isEmpty())
                    return@mapNotNull null


                scope.queueDataSet(holderId, data)
            } catch (e: Exception) {
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
            } catch (e: NumberFormatException) {
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
        } catch (e: NumberFormatException) {
            context.respondError(Code.FIELD_IS_NOT_NUMBER)
            return
        }

        context.respond(PacketTopPosition(entry))
    }

    suspend fun loadData(scope: Scope, holderId: String): NexusData {
        val session = SessionsManager.getExistingSession(scope.id, holderId)
        if (session != null) {
            val node = session.node
            if (node != this) {
                node.requestSync(session.scope, session.holderId)
            }
        }

        return scope.loadData(holderId)
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

    /**
     * Request data synchronisation (actual data) for given holder in the given scope.
     *
     * The next code called after this function will be executed after the data is synced.
     */
    suspend fun requestSync(scope: Scope, holderId: String) {
        val syncPacket = PacketRequestHolderSync(scope.id, holderId)

        @Suppress( "RemoveExplicitTypeArguments")
        sendPacket<Unit>(syncPacket) {
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
                } catch (e: Exception) {
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

            receive<PacketError> {
                if (packet.code == Code.SESSION_NOT_FOUND) {
                    logger.warn("Session not found while syncing data with node $this for holder $holderId. Packet: $packet. Removing session...")
                    stopSession(scope.id, holderId)
                } else {
                    logger.error("Error returned while syncing data: $packet")
                }
            }

            timeout(5000) {}

            exception {
                logger.error("Exception occurred while syncing data: $exception")
            }
        }.await()
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