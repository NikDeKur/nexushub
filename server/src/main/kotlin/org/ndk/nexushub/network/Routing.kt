package org.ndk.nexushub.network

import io.ktor.server.application.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.NexusHub.blockingScope
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.auth.AuthenticationManager
import org.ndk.nexushub.node.KtorTalker
import org.ndk.nexushub.node.NodesManager
import org.ndk.nexushub.node.TalkersManager
import java.time.Duration
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

fun Application.configureRouting() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        route("connection") {
            routeProtocol()
        }
    }
}

fun Route.routeProtocol() {
    val activeConnections = AtomicInteger(0)

    webSocket {
        // Возможная причина проблем:
        // Хакеры закинут много подключений и сервер будет отклонять подключения от нормальных нод
        // Из идей, можно давать ограниченное время на авторизацию и отключать ноду, если она не успела авторизоваться
        // Даже если хакеры попробуют заспамить, то нормальные ноды смогут подключиться

        if (activeConnections.incrementAndGet() > NexusHub.MAX_CONNECTIONS) {
            close(CloseReason(CloseReason.Codes.TRY_AGAIN_LATER, "Too many connections"))
            return@webSocket
        }

        val address = addressHash
        try {
            for (frame in incoming) {
                if (frame !is Frame.Binary) continue
                val talker = TalkersManager.getExistingTalker(address) ?: run {
                    val newTalker = KtorTalker(this)
                    TalkersManager.setTalker(address, newTalker)
                    newTalker
                }

                // Run as new coroutine to avoid blocking and handle multiple connections
                blockingScope.launch {
                    val packet = talker.receive(frame.readBytes()) ?: return@launch
                    AuthenticationManager.executePacket(talker, packet)
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {
            // Connection closed
            TalkersManager.getExistingTalker(address)?.let { talker ->
                val node = NodesManager.getAuthenticatedNode(talker)
                if (node != null) {
                    logger.info("Node ${node.id} disconnected")
                    node.cleanUp()
                } else {
                    TalkersManager.removeTalker(address)
                }
            }

            activeConnections.decrementAndGet()
        }
    }
}

inline val DefaultWebSocketServerSession.addressHash: Int
    get() = call.request.origin.let {
        Objects.hash(it.remoteAddress, it.remotePort)
    }
