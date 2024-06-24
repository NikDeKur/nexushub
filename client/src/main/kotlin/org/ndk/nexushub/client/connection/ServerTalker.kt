package org.ndk.nexushub.client.connection

import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.ndk.nexushub.network.PacketManager
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.Packet
import java.util.*

class ServerTalker(
    val websocket: DefaultClientWebSocketSession,
    val networkDispatcher: CoroutineDispatcher
): Talker {

    override val addressHash = websocket.call.request.url.let {
        Objects.hash(it.host, it.port)
    }

    override val addressStr = websocket.call.request.url.let {
        "${it.host}:${it.port}"
    }


    val packetManager = PacketManager(this, networkDispatcher)

    override val isOpen: Boolean
        get() = websocket.closeReason.isActive

    override suspend fun send(transmission: PacketTransmission<*>) {
        withContext(networkDispatcher) {
            val bytes = packetManager.processOutgoingTransmission(transmission)
            websocket.send(bytes)
        }
    }

    override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
        return packetManager.processIncomingPacket(data)
    }

    override suspend fun close(code: Short, reason: String) {
        withContext(networkDispatcher) {
            websocket.close(CloseReason(code, reason))
        }
    }
}

// Hardcoded for ktor websocket.
// Remove if nexushub will be used with other websocket implementations
suspend inline fun Talker.close(code: CloseReason.Codes, reason: String) {
    close(code.code, reason)
}