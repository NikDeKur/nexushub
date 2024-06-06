@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.node

import io.ktor.websocket.*
import org.ndk.global.interfaces.Snowflake
import org.ndk.klib.forEachSafe
import org.ndk.klib.removeEmpty
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.auth.account.Account
import org.ndk.nexushub.exception.NoScopeAccessException
import org.ndk.nexushub.network.GsonSupport
import org.ndk.nexushub.network.NexusData
import org.ndk.nexushub.network.dsl.HandlerContext
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.packet.*
import org.ndk.nexushub.network.packet.PacketError.Level
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.scope.Scope
import org.ndk.nexushub.scope.ScopesManager
import org.ndk.nexushub.session.Session
import org.ndk.nexushub.session.SessionsManager

class ClientNode(
    val talker: Talker,
    override val id: String,
    val account: Account,
) : Snowflake<String>, Talker by talker {

    var isAlive = true

    suspend fun processAuthenticatedPacket(context: HandlerContext.Receive<Packet, Unit>) {
        val packet = context.packet

        try {
            @Suppress("UNCHECKED_CAST")
            when (packet) {
                is PacketCreateSession -> createSession(context as IncomingContext<PacketCreateSession>)
                is PacketLoadData -> processLoadDataPacket(context as IncomingContext<PacketLoadData>)
                is PacketStopSession -> stopSession(context as IncomingContext<PacketStopSession>)
                is PacketSaveData -> processSaveDataPacket(context as IncomingContext<PacketSaveData>)
                is PacketRequestLeaderboard -> processRequestLeaderboardPacket(context as IncomingContext<PacketRequestLeaderboard>)
            }
        } catch (e: NoScopeAccessException) {
            context.respond(PacketError(Level.ERROR, "Scope ${e.scopeId} is not allowed for your account"))
        }
    }

    inline fun hasScopeAccess(scopeId: String): Boolean {
        return account.allowedScopes.contains(scopeId)
    }

    inline fun checkScopeAccess(scopeId: String) {
        if (!hasScopeAccess(scopeId)) {
            throw NoScopeAccessException(scopeId)
        }
    }

    suspend inline fun getScopeWithAccess(scopeId: String): Scope {
        checkScopeAccess(scopeId)
        return ScopesManager.getScope(scopeId)
    }

    suspend fun createSession(context: IncomingContext<PacketCreateSession>) {
        val packet = context.packet
        val holderId = packet.holderId

        val scope = getScopeWithAccess(packet.scopeId)
        val sessions = SessionsManager.getExistingSession(scope.id, holderId)
        if (sessions != null) {
            context.respond(PacketError(Level.ERROR, "Session already exists"))
            return
        }

        val dataStr = try {
            val data = loadData(scope, holderId)
            GsonSupport.dataToString(data)
        } catch (e: Exception) {
            e.printStackTrace()
            context.respond(PacketError(Level.ERROR, "Error while serializing data"))
            return
        }

        SessionsManager.startSession(this, scope, holderId)

        val dataPacket = PacketUserData(holderId, scope.id, dataStr)
        context.respond(dataPacket)
    }

    suspend fun stopSession(context: IncomingContext<PacketStopSession>) {
        val packet = context.packet
        val holderId = packet.holderId

        val scope = getScopeWithAccess(packet.scopeId)

        // Save data and stop session
        try {
            setData(scope, holderId, packet.data)
        } catch (e: Exception) {
            context.respond(PacketError(Level.ERROR, "Error while saving data"))
            return
        }

        context.respond(PacketOk("Session stopped"))
    }

    suspend fun processLoadDataPacket(context: IncomingContext<out PacketLoadData>) {
        val packet = context.packet
        val holderId = packet.holderId

        val scope = getScopeWithAccess(packet.scopeId)

        val dataStr = try {
            val data = loadData(scope, holderId)
            GsonSupport.dataToString(data)
        } catch (e: Exception) {
            e.printStackTrace()
            context.respond(PacketError(Level.ERROR, "Error while serializing data"))
            return
        }

        val dataPacket = PacketUserData(holderId, scope.id, dataStr)
        context.respond(dataPacket)
    }

    suspend fun processSaveDataPacket(context: IncomingContext<out PacketSaveData>) {
        val packet = context.packet
        val holderId = packet.holderId
        val scope = getScopeWithAccess(packet.scopeId)

        val dataStr = packet.data

        try {
            setData(scope, holderId, dataStr)
        } catch (e: Exception) {
            val errors = mapOf(holderId to dataStr)
            context.respond(PacketSaveError("Error while saving data", errors))
            return
        }

        context.respond(PacketOk("Data saved"))
    }

    suspend fun processBatchSaveDataPacket(context: IncomingContext<PacketBatchSaveData>) {
        val packet = context.packet
        val scope = getScopeWithAccess(packet.scopeId)

        val data = packet.data

        val errors = HashMap<String, String>()

        data.forEachSafe({ errors[key] = value }) { (holderId, dataStr) ->
            setData(scope, holderId, dataStr)
        }

        if (errors.isNotEmpty()) {
            val error = PacketSaveError("Error while saving some of data", errors)
            context.respond(error)
        } else {
            context.respond(PacketOk("Data saved gracefully"))
        }
    }

    suspend fun processRequestLeaderboardPacket(context: IncomingContext<PacketRequestLeaderboard>) {
        val packet = context.packet
        val field = packet.field
        val limit = packet.limit
        val scope = getScopeWithAccess(packet.scopeId)

        val session = SessionsManager.getExistingSession(scope.id, packet.holderId)
        if (session != null && session.node != this)
            requestSync(scope.id)

        val leaderboard = scope.getLeaderboard(field, limit)

        context.respond(PacketLeaderboard(leaderboard))
    }

    suspend fun processTopPositionPacket(context: IncomingContext<PacketRequestTopPosition>) {
        val packet = context.packet
        val holderId = packet.holderId
        val field = packet.field
        val scope = getScopeWithAccess(packet.scopeId)

        val session = SessionsManager.getExistingSession(scope.id, holderId)
        if (session != null && session.node != this)
            requestSync(scope.id)

        val entry = scope.getTopPosition(holderId, field)

        val pos = entry?.position ?: -1
        val value = entry?.value ?: -1.0

        context.respond(PacketTopPosition(pos, value))
    }

    suspend fun loadData(scope: Scope, holderId: String): NexusData {
        val session = SessionsManager.getExistingSession(scope.id, holderId)
        if (session != null && session.node != this) {
            requestSync(session)
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
    fun setData(scope: Scope, holderId: String, dataStr: String) {
        val dataRaw = GsonSupport.dataFromString(dataStr)

        val data = dataRaw.removeEmpty(maps = true, collections = true)

        scope.setData(holderId, data)
    }


    suspend fun requestSync(scopeId: String) {
        val packet = PacketRequestSync(scopeId)

        sendPacket(packet) {
            receive<PacketOk> {}
            receive<PacketError> {
                logger.warn("Error while syncing data: ${this.packet}")
            }

            timeout(5000) {
                logger.warn("Timeout while global syncing data for scope $scopeId with node $id")
            }

            exception {
                logger.warn("Exception while syncing data!", exception)
            }
        }.await()
    }


    /**
     * After a client receives this packet, it will send requested data to the server with [PacketSaveData]
     *
     * The answer would not be responded to this function, but this function will wait for PacketOk or PacketError.
     *
     * The next code called after this function will be executed after the data is synced.
     */
    suspend fun requestSync(session: Session) {
        val holderId = session.holderId
        val node = session.node
        val syncPacket = PacketRequestHolderSync(id, holderId)

        node.sendPacket(syncPacket) {
            receive<PacketOk> {}
            receive<PacketError> {
                logger.error("Error returned while syncing data: $packet")
            }

            timeout(5000) {}

            exception {
                logger.error("Exception occurred while syncing data: $exception")
            }
        }.await()
    }


    override fun toString(): String {
        return "ClientNode(id='$id', account=$account)"
    }

    suspend fun ping(): Boolean {
        return sendPacket(PacketPing()) {
            // Response is a ping packet
            receive<PacketPong> {
                true
            }

            // Response is not a ping packet
            receive {
                logger.warn("Received unexpected packet while pinging node: $packet")
                false
            }

            // Timeout exceeded
            timeout(4000) {
                logger.warn("Timeout while pinging node")
                false
            }

            // Exception occurred
            exception {
                logger.error("Exception occurred while pinging node", exception)
                false
            }

        }.await()
    }


    suspend fun checkAlive() {
        if (!isAlive) return
        if (!ping()) {
            stopAsInactive()
        }
    }

    suspend fun stopAsInactive() {
        isAlive = false
        close(CloseReason.Codes.GOING_AWAY, "Inactive")
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