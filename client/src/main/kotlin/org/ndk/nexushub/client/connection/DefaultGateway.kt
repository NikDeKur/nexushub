package org.ndk.nexushub.client.connection

import dev.nikdekur.ndkore.ext.toReadableString
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.dsl.PacketReaction
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.*
import org.ndk.nexushub.util.CloseCode
import org.slf4j.LoggerFactory
import kotlin.time.toJavaDuration

open class DefaultGateway(
    val configuration: GatewayConfiguration,
    val packetsFlow: MutableSharedFlow<IncomingContext<out Packet>>,
    val onReady: suspend () -> Unit = {},
) {

    val logger = LoggerFactory.getLogger("NexusHubGateway")

    var client: HttpClient? = null
    var talker: ServerTalker? = null

    var state = ConnectionState.NOT_CONNECTED

    val readingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    suspend fun <R> sendPacket(packet: Packet, block: PacketReaction.Builder<R>.() -> Unit): PacketTransmission<R> {
        if (state != ConnectionState.CONNECTED)
            throw NexusException.NotConnected("Trying to send packet ($packet) while not connected.", "")

        return talker!!.sendPacket(packet, block)
    }

    suspend fun connect() {
        val retry = configuration.retry

        val client = HttpClient {
            install(WebSockets)
        }

        this.client = client

        while (retry.hasNext && state != ConnectionState.CONNECTED) {
            logger.info("Connecting to NexusHub...")

            state = ConnectionState.CONNECTING

            val socket = try {
                client.webSocketSession(
                    HttpMethod.Get,
                    host = configuration.host,
                    port = configuration.port,
                    path = "/connection",
                )
            } catch (e: Exception) {
                val retryAfter = retry.retryDuration()
                val retryAfterStr = retryAfter.toJavaDuration().toReadableString("en")
                logger.warn("Failed to connect to NexusHub. Retrying in $retryAfterStr", e)
                delay(retryAfter)
                continue
            }


            talker = ServerTalker(socket, Dispatchers.IO)

            state = ConnectionState.CONNECTED

            readingScope.launch {
                onReady()
            }


            retry.reset()

            // Incoming would end on close
            try {
                socket.incoming.consumeEach {
                    if (it !is Frame.Binary) return@consumeEach

                    // Run as new coroutine to avoid blocking and handle multiple operations
                    readingScope.launch consume@{
                        val context = talker!!.receive(it.readBytes()) ?: return@consume
                        packetsFlow.emit(context)
                    }
                }
            } catch (e: Exception) {
                logger.error("", e)
            }

            // Обработка закрытия канала
            handleClose()

            val retryAfter = retry.retryDuration()
            val retryAfterStr = retryAfter.toJavaDuration().toReadableString("en")
            logger.warn("Failed to connect to NexusHub. Retrying in $retryAfterStr")
            delay(retryAfter)
        }


        finish()
        throw ConnectException.NoResponse("Failed to connect to NexusHub!")
    }

    private suspend fun handleClose() {

        val reason = withTimeoutOrNull(1500) {
            talker?.websocket?.closeReason?.await()
        }

        state = ConnectionState.NOT_CONNECTED

        if (reason == null) {
            logger.error("WebSocket channel closed without reason")
            return
        }

        val code = reason.code
        val enum = CloseCode.entries.getOrNull(code.toInt() - 4000)
        if (enum == null) {
            logger.warn("WebSocket channel closed with unknown reason. Code: $code")
            return
        }

        val comment = reason.message

        when (enum) {
            CloseCode.WRONG_CREDENTIALS ->
                throw ConnectException.WrongCredentials(
                    "Server rejected connection due to wrong credentials",
                    comment
                )


            CloseCode.INVALID_DATA ->
                throw ConnectException.InvalidData(
                    "Server rejected connection due to invalid authentication data",
                    comment
                )

            CloseCode.NODE_ALREADY_EXISTS ->
                throw ConnectException.NodeAlreadyExists(
                    "Server rejected connection due to node already exists",
                    comment
                )


            CloseCode.AUTHENTICATION_TIMEOUT ->
                // Ignore, we could attempt to reconnect and fit in time
                logger.warn("Server rejected connection due to authentication timeout: $comment")


            CloseCode.TOO_MANY_CONNECTIONS ->
                // Ignore, because it's not a connection problem
                logger.warn("Server rejected connection due to too many connections: $comment")


            CloseCode.NODE_IS_NOT_AUTHENTICATED ->
                // Ignore, because it's not a connection problem
                logger.warn("Server rejected connection because of trying to perform operation, " +
                        "that requires authentication, without authentication: $comment")


            CloseCode.UNEXPECTED_BEHAVIOUR ->
                // Ignore, because it's not a connection problem
                logger.warn("Server rejected connection due to unexpected behaviour: $comment")

        }
    }





    protected fun finish() {
        state = ConnectionState.NOT_CONNECTED
        client?.close()
        client = null
        talker = null
    }

    suspend fun disconnect() {
        talker?.close(CloseReason.Codes.NORMAL, "NexusHub client disconnection")
        finish()
    }
}

