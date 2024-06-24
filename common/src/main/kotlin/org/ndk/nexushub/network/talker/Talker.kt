package org.ndk.nexushub.network.talker

import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.dsl.PacketReaction
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.Packet

interface Talker {

    val addressHash: Int
    val addressStr: String

    val isOpen: Boolean

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

    suspend fun close(code: Short, reason: String)

}