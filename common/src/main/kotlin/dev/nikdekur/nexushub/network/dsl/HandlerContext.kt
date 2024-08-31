/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.dsl

import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet

interface HandlerContext<R> {

    val talker: Talker


    interface Responsible<P> : HandlerContext<P> {
        val packet: Packet

        suspend fun <R> respond(
            packet: Packet,
            builder: PacketReaction.Builder<R>.() -> Unit = {}
        ): PacketTransmission<R> {
            return talker.send(packet, builder, this.packet.sequantial)
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
        override val packet: P,
        val isResponse: Boolean
    ) : Responsible<R>

}

typealias IncomingContext<T> = HandlerContext.Receive<out T, Unit>


