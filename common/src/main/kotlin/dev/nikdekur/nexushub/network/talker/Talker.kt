/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.talker

import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.util.CloseCode

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

    /**
     * Close the talker.
     *
     * @param code The close code.
     * @param comment The close comment.
     */
    suspend fun close(code: CloseCode, comment: String = "")

}