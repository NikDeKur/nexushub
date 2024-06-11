package org.ndk.nexushub.client.connection

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.packet.*
import org.ndk.nexushub.packet.PacketError.Level

class NexusHubConnection(val hub: NexusHub) {

    val client = HttpClient {
        install(WebSockets)
    }

    val logger = hub.logger

    var talker: Talker? = null

    var isConnected = false
    var isAuth = false


    suspend fun connect() {
        withContext(hub.blockingScope.coroutineContext) {
            val data = hub.builder

            val socket = try {
                client.webSocketSession(
                    HttpMethod.Get,
                    host = data.host,
                    port = data.port,
                    path = "/connection",
                )
            } catch (e: Exception) {
                throw ConnectException.NoResponseException("No response from server during connection attempt")
            }


            talker = ServerTalker(hub, socket, hub.blockingScope)

            isConnected = true

            socket.apply {
                hub.blockingScope.launch {
                    incoming.consumeEach {
                        if (it !is Frame.Binary) return@consumeEach

                        // Run as new coroutine to avoid blocking and handle multiple operations
                        hub.blockingScope.launch consume@ {
                            val context = talker!!.receive(it.readBytes()) ?: return@consume
                            onPacketReceived(context)
                        }
                    }

                    // Обработка закрытия канала
                    val reason = closeReason.await()
                    onClose(reason)
                }

                NexusAuthentication(this@NexusHubConnection).start()

                logger.info("Authenticated successfully. Connection established.")
                isAuth = true
            }
        }
    }

    fun onClose(reason: CloseReason?) {
        if (reason == null) {
            logger.error("WebSocket channel closed without reason")
            return
        }

        val code  = reason.code
        when (code) {
            4000.toShort() -> {
                throw ConnectException.WrongCredentialsException("Server rejected connection due to wrong credentials")
            }
            else -> {
                logger.warn("WebSocket channel closed. Reason: ${reason.message}")
                return
            }
        }
    }

    /**
     * Function to runtime handle fatal errors.
     *
     * Function will log the error and gracefully disconnect from the server.
     *
     * @param message Error message / Comment
     * @param e Exception that caused the error
     */
    internal suspend fun fatalError(message: String, e: Exception? = null) {
        logger.error("Fatal error occurred: $message", e)
        if (isConnected) {
            if (isAuth)
                hub.stopServices()

            disconnect()
        }
    }





    suspend fun disconnect() {
        isConnected = false
        isAuth = false
        talker?.close(CloseReason.Codes.NORMAL, "NexusHub client disconnection")
        client.close()
    }


    suspend fun processSyncHolderData(context: IncomingContext<PacketRequestHolderSync>) {
        val packet = context.packet
        val scope = packet.scope
        val holderId = packet.holderId
        val service = hub.getService(scope) ?: return
        val session = service.getExistingSession(holderId)
        if (session == null) {
            context.respond(
                PacketError(
                    Level.ERROR,
                    PacketError.Code.SESSION_NOT_FOUND,
                    "No session found"
                )
            )
            return
        }

        if (!session.hasToBeSaved())
            return


        session.afterLoadHooks.executeHooks()
        val dataStr = session.serialiseData()
        val savePacket = PacketSaveData(scope, holderId, dataStr)

        // Expected behaviour is to receive nothing after sync
        context.respond(savePacket) {
            receive {
                logger.warn("Unexpected behaviour while syncing data for $scope:$holderId. Received: $packet")
            }

            exception {
                logger.warn("Exception while syncing data!", exception)
            }
        }
    }

    suspend fun processSyncData(context: IncomingContext<PacketRequestSync>) {
        val packet = context.packet
        val scopeId = packet.scope
        val service = hub.getService(scopeId) ?: return

        val batchMap = HashMap<String, String>()

        val batchPacket = PacketBatchSaveData(scopeId, batchMap)
        service.sessions.forEach {
            if (!it.hasToBeSaved()) return@forEach

            // Invoke any hooks before saving the data
            it.beforeSaveHooks.executeHooks()
            val dataStr = it.serialiseData()
            batchMap[it.holderId] = dataStr
        }

        context.respond(batchPacket)
    }


    suspend fun processPing(context: IncomingContext<PacketPing>) {
        context.respond(PacketPong())
    }

    suspend fun onPacketReceived(context: IncomingContext<Packet>) {
        val packet = context.packet
        logger.debug("Packet received: $packet")

        // If the packet is a response, we don't need to process it
        if (context.isResponse)
            return

        @Suppress("UNCHECKED_CAST")
        when (packet) {
            is PacketPing -> processPing(context as IncomingContext<PacketPing>)
            is PacketRequestHolderSync -> processSyncHolderData(context as IncomingContext<PacketRequestHolderSync>)
            is PacketRequestSync -> processSyncData(context as IncomingContext<PacketRequestSync>)
            else -> logger.warn("Unhandled packet: $packet")
        }
    }
}

