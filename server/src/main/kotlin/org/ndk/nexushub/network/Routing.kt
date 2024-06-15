package org.ndk.nexushub.network

import dev.nikdekur.ndkore.ext.debug
import dev.nikdekur.ndkore.ext.info
import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.ndk.nexushub.NexusHub.blockingScope
import org.ndk.nexushub.NexusHub.config
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.auth.AuthenticationManager
import org.ndk.nexushub.node.KtorTalker
import org.ndk.nexushub.node.TalkersManager
import org.ndk.nexushub.node.close
import org.ndk.nexushub.util.CloseCode
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun Application.configureRouting() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(10)
        timeout = Duration.ofSeconds(10)
    }

    routing {
        route("connection") {
            routeProtocol()
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Route.routeProtocol() {
    val activeConnections = AtomicInteger(0)

    webSocket {
        // Возможная причина проблем:
        // Хакеры закинут много подключений и сервер будет отклонять подключения от нормальных нод
        // Из идей, можно давать ограниченное время на авторизацию и отключать ноду, если она не успела авторизоваться
        // Даже если хакеры попробуют заспамить, то нормальные ноды смогут подключиться


        val address = addressHash

        if (activeConnections.incrementAndGet() > config.network.max_connections) {
            close(CloseReason(CloseCode.TOO_MANY_CONNECTIONS.code, "Too many connections already exists, try again later"))
            TalkersManager.cleanUp(address)
            return@webSocket
        }

        val talker = KtorTalker(this)
        TalkersManager.setTalker(address, talker)

        AuthenticationManager.waitForAuthentication(talker)

        try {
            incoming.consumeEach { frame ->
                if (frame !is Frame.Binary) return@consumeEach

                if (!talker.isOpen) return@consumeEach

                // Run as new coroutine to avoid blocking and handle multiple connections
                blockingScope.launch {
                    val context = talker.receive(frame.readBytes()) ?: return@launch

                    logger.debug { "[$address] Received packet: ${context.packet}" }

                    try {
                        AuthenticationManager.executePacket(talker, context)
                    } catch (e: CancellationException) {
                       // Connection closed while a processing packet
                       // Do nothing
                    } catch (e: Exception) {
                        logger.info { "Error while processing packet: $e" }
                        e.printStackTrace()
                    }
                }
            }

        } catch (e: Throwable) {
            e.printStackTrace()

        } finally {
            if (isActive) {
                talker.close(CloseReason.Codes.INTERNAL_ERROR, "Exception occurred")
            }

            activeConnections.decrementAndGet()
        }
    }
}

inline val DefaultWebSocketServerSession.addressHash: Int
    get() = call.request.origin.let {
        Objects.hash(it.remoteAddress, it.remotePort)
    }
