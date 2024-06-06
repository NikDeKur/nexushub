package org.ndk.nexushub.network.dsl

import org.ndk.nexushub.network.packet.Packet
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.network.transmission.PacketTransmission

interface HandlerContext<R> {

    val talker: Talker


    interface Responsible<R> : HandlerContext<R> {
        val packet: Packet

        suspend fun respond(packet: Packet, builder: PacketReaction.Builder<R>.() -> Unit = {}) {
            val reaction = PacketReaction.Builder<R>()
                .apply(builder)
                .build()
            val transmission = PacketTransmission(
                packet,
                reaction
            )
            transmission.respondTo = this.packet
            talker.send(transmission)
        }
    }


    open class Timeout<R>(
        override val talker: Talker,
        override val packet: Packet
    ) : Responsible<R>

    open class Exception<R>(
        override val talker: Talker,
        override val packet: Packet,
        val exception: kotlin.Exception
    ) : Responsible<R>

    open class Receive<P : Packet, R>(
        override val talker: Talker,
        override val packet: P
    ) : Responsible<R>

    open class Incoming<P : Packet>(
        talker: Talker,
        packet: P
    ) : Receive<P, Unit>(talker, packet)
}

typealias IncomingContext<T> = HandlerContext.Incoming<T>


