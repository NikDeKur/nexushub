package org.ndk.nexushub.network.talker

import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.dsl.PacketReaction
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.Packet

interface Talker {

    val addressHash: Int
    val addressStr: String

    val isOpen: Boolean

    /**
     * Represent if the talker is blocked from sending packets.
     *
     * The server could set this to prevent the talker from sending packets.
     */
    val isBlocked: Boolean

    suspend fun send(transmission: PacketTransmission<*>)

    suspend fun <R> sendPacket(
        packet: Packet,
        builder: PacketReaction.Builder<R>.() -> Unit = {}
    ): PacketTransmission<R> {
        val reaction = PacketReaction.Builder<R>().apply(builder).build()
        val transmission = PacketTransmission(packet, reaction)
        send(transmission)
        return transmission
    }


    suspend fun receive(data: ByteArray): IncomingContext<Packet>?

    /**
     * Close the talker.
     *
     * @param code The close code.
     * @param reason The close reason.
     * @param block If true, will block talker from sending packets.
     */
    suspend fun close(code: Short, reason: String, block: Boolean)

}