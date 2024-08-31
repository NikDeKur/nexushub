/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.talker

import dev.nikdekur.ndkore.ext.debug
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.PacketController
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.send
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import java.util.function.Predicate

abstract class PacketControllerTalker(override val address: Address) : AbstractTalker() {

    abstract val packetController: PacketController

    abstract suspend fun send(data: ByteArray)

    override suspend fun <R> send(
        packet: Packet,
        reaction: PacketReaction.Builder<R>.() -> Unit,
        respondTo: UShort?
    ): PacketTransmission<R> {
        logger.debug { "[$address] Sending packet $packet" }

        return packetController.send(packet, reaction, respondTo).also {
            val bytes = it.second
            send(bytes)
        }.first
    }

    override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
        return packetController.processReceiving(data)

    }

    override suspend fun <T : Packet> wait(packetClass: Class<T>, condition: Predicate<T>): IncomingContext<T> {
        return packetController.wait(packetClass, condition)
    }

}