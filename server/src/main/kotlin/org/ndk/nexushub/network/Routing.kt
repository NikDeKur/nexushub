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
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.NexusHub.blockingScope
import org.ndk.nexushub.auth.AuthenticationManager
import org.ndk.nexushub.network.ratelimit.PeriodRateLimiter
import org.ndk.nexushub.node.KtorTalker
import org.ndk.nexushub.node.TalkersManager
import org.ndk.nexushub.node.close
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*
import kotlin.time.Duration.Companion.seconds

val logger = LoggerFactory.getLogger("NexusHubRouting")

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

    val config = NexusHub.config.network.rateLimit

    val rateLimiter = PeriodRateLimiter(
        limit = config.maxRequests,
        period = config.timeWindow.seconds
    )

    webSocket {
        val address = addressHash
        val addressStr = addressStr

        val talker = KtorTalker(this)
        TalkersManager.setTalker(address, talker)

        try {
            incoming.consumeEach { frame ->
                if (talker.isBlocked) return@consumeEach

                if (!rateLimiter.acquire(talker)) {
                    logger.info("[$addressStr] Rate limit exceeded")
                    talker.close(CloseReason.Codes.VIOLATED_POLICY, "Rate limit exceeded", true)
                    return@consumeEach
                }

                if (frame !is Frame.Binary) return@consumeEach

                // Run as new coroutine to avoid blocking and handle multiple connections
                blockingScope.launch {
                    val context = talker.receive(frame.readBytes()) ?: return@launch

                    logger.debug { "[$addressStr] Received packet: ${context.packet}" }

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
            logger.warn("[$addressStr] Exception occurred!", e)
        } finally {
            if (isActive) {
                talker.close(CloseReason.Codes.INTERNAL_ERROR, "Exception occurred", true)
            }
        }
    }
}

inline val DefaultWebSocketServerSession.addressHash: Int
    get() = call.request.origin.let {
        Objects.hash(it.remoteAddress, it.remotePort)
    }

inline val DefaultWebSocketServerSession.addressStr: String
    get() = call.request.origin.let {
        "${it.remoteAddress}:${it.remotePort}"
    }