package org.ndk.nexushub.client.connection

import dev.nikdekur.ndkore.scheduler.Scheduler
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.network.PacketManager
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.Packet
import java.util.*

class ServerTalker(
    val hub: NexusHub,
    val websocket: DefaultClientWebSocketSession,
    scheduler: Scheduler
): Talker {

    override val addressHash = websocket.call.request.url.let {
        Objects.hash(it.host, it.port)
    }

    val packetManager = PacketManager(this, scheduler)

    override val isOpen: Boolean
        get() = websocket.isActive

    override suspend fun send(transmission: PacketTransmission<*>) {
        withContext(hub.blockingScope.coroutineContext) {
            val bytes = packetManager.processOutgoingTransmission(transmission)
            websocket.send(bytes)
        }
    }

    override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
        return packetManager.processIncomingPacket(data)
    }

    override suspend fun close(code: Short, reason: String) {
        withContext(hub.blockingScope.coroutineContext) {
            websocket.close(CloseReason(code, reason))
        }
    }
}

// Hardcoded for ktor websocket.
// Remove if nexushub will be used with other websocket implementations
suspend inline fun Talker.close(code: CloseReason.Codes, reason: String) {
    close(code.code, reason)
}