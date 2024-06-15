package org.ndk.nexushub.node

import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.network.PacketManager
import org.ndk.nexushub.network.addressHash
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.Packet

class KtorTalker(val session: DefaultWebSocketServerSession) : Talker {

    override val addressHash = session.addressHash

    val packetManager = PacketManager(this, NexusHub.blockingScope)

    override val isOpen: Boolean
        get() = session.isActive

    override suspend fun send(transmission: PacketTransmission<*>) {
        withContext(NexusHub.blockingScope.coroutineContext) {
            val bytes = packetManager.processOutgoingTransmission(transmission)
            session.send(bytes)
        }
    }

    override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
        return packetManager.processIncomingPacket(data)
    }

    override suspend fun close(code: Short, reason: String) {
        withContext(NexusHub.blockingScope.coroutineContext) {
            session.close(CloseReason(code, reason))
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


suspend inline fun Talker.close(code: CloseReason.Codes, reason: String) {
    close(code.code, reason)
}