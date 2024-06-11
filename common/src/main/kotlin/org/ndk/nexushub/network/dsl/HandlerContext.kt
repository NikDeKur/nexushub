package org.ndk.nexushub.network.dsl

import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.network.transmission.PacketTransmission

interface HandlerContext<R> {

    val talker: Talker


    interface Responsible<R> : HandlerContext<R> {
        val packet: org.ndk.nexushub.packet.Packet

        suspend fun respond(packet: org.ndk.nexushub.packet.Packet, builder: PacketReaction.Builder<R>.() -> Unit = {}): PacketTransmission<R> {
            val reaction = PacketReaction.Builder<R>()
                .apply(builder)
                .build()
            val transmission = PacketTransmission(
                packet,
                reaction
            )
            transmission.respondTo = this.packet
            talker.send(transmission)
            return transmission
        }
    }


    open class Timeout<R>(
        override val talker: Talker,
        override val packet: org.ndk.nexushub.packet.Packet
    ) : Responsible<R>

    open class Exception<R>(
        override val talker: Talker,
        override val packet: org.ndk.nexushub.packet.Packet,
        val exception: kotlin.Exception
    ) : Responsible<R>

    open class Receive<P : org.ndk.nexushub.packet.Packet, R>(
        override val talker: Talker,
        override val packet: P,
        val isResponse: Boolean
    ) : Responsible<R>

}

typealias IncomingContext<T> = HandlerContext.Receive<out T, Unit>


