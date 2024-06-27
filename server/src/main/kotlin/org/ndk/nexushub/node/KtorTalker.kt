package org.ndk.nexushub.node

import dev.nikdekur.ndkore.ext.debug
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.NexusHub.logger
import org.ndk.nexushub.network.PacketManager
import org.ndk.nexushub.network.addressHash
import org.ndk.nexushub.network.addressStr
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.Packet

class KtorTalker(val websocket: DefaultWebSocketServerSession) : Talker {

    override val addressHash = websocket.addressHash
    override val addressStr = websocket.addressStr


    val packetManager = PacketManager(this, Dispatchers.IO)

    override val isOpen: Boolean
        get() = websocket.closeReason.isActive
    override var isBlocked: Boolean = false

    override suspend fun send(transmission: PacketTransmission<*>) {
        withContext(NexusHub.blockingScope.coroutineContext) {
            val bytes = packetManager.processOutgoingTransmission(transmission)
            logger.debug { "[$addressStr] Sending packet ${transmission.packet}" }
            websocket.send(bytes)
        }
    }

    override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
        return packetManager.processIncomingPacket(data)
    }

    override suspend fun close(code: Short, reason: String, block: Boolean) {
        this.isBlocked = block
        withContext(NexusHub.blockingScope.coroutineContext) {
            websocket.close(CloseReason(code, reason))
            TalkersManager.cleanUp(addressHash)
        }
    }




    override fun equals(other: Any?): Boolean {
        if (other !is KtorTalker) return false
        return this.addressHash == other.addressHash
    }

    override fun hashCode(): Int {
        return addressHash
    }

    override fun toString(): String {
        return "KtorTalker(address='$addressHash')"
    }
}


suspend inline fun Talker.close(code: CloseReason.Codes, reason: String, block: Boolean) {
    close(code.code, reason, block)
}