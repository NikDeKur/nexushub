/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.dsl.PacketReactionBuilder
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import java.util.function.Predicate

interface PacketController {

    suspend fun <R> newTransmission(
        packet: Packet,
        reaction: PacketReaction<R>,
        respondTo: UShort?
    ): PacketTransmission<R>

    suspend fun processSending(transmission: PacketTransmission<*>): ByteArray
    suspend fun processReceiving(bytes: ByteArray): IncomingContext<Packet>?

    suspend fun <T : Packet> wait(packetClass: Class<T>, condition: Predicate<T>): IncomingContext<T>
}

suspend inline fun <R> PacketController.send(
    packet: Packet,
    reaction: PacketReactionBuilder<R>,
    respondTo: UShort? = null
): Pair<PacketTransmission<R>, ByteArray> {
    val reaction = PacketReaction.Builder<R>()
        .apply(reaction)
        .build()

    val transmission = newTransmission(packet, reaction, respondTo)
    val bytes = processSending(transmission)

    return transmission to bytes
}