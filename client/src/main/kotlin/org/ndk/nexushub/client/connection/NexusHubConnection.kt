package org.ndk.nexushub.client.connection

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.network.dsl.HandlerContext
import org.ndk.nexushub.network.packet.*
import org.ndk.nexushub.network.packet.PacketError.Level
import org.ndk.nexushub.network.talker.Talker

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
                    logger.warn("WebSocket channel closed. Reason: ${reason?.message}")
                }

                NexusAuthentication(this@NexusHubConnection).start()

                logger.info("Authenticated successfully. Connection established.")
                isAuth = true
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


    suspend fun syncHolderData(context: HandlerContext.Incoming<PacketRequestHolderSync>) {
        val packet = context.packet
        val scope = packet.scope
        val holderId = packet.holderId
        val service = hub.getCorrespondingService(scope) ?: return
        val session = service.getExistingSession(holderId)
        if (session == null) {
            context.respond(PacketError(Level.ERROR, "No data found"))
            return
        }

        session.saveData()

        context.respond(PacketOk("Data synced"))
    }

    suspend fun syncData(context: HandlerContext.Incoming<PacketRequestSync>) {
        val packet = context.packet
        val scope = packet.scope
        val service = hub.getCorrespondingService(scope) ?: return

        service.sessions.forEach {
            it.saveData()
        }

        context.respond(PacketOk("Data synced"))
    }

    suspend fun processPing(context: HandlerContext.Incoming<PacketPing>) {
        context.respond(PacketPong())
    }

    suspend fun onPacketReceived(context: HandlerContext.Incoming<Packet>) {
        val packet = context.packet
        logger.debug("Packet received: $packet")

        @Suppress("UNCHECKED_CAST")
        when (packet) {
            is PacketPing -> processPing(context as HandlerContext.Incoming<PacketPing>)
            is PacketRequestHolderSync -> syncHolderData(context as HandlerContext.Incoming<PacketRequestHolderSync>)
        }
    }
}

