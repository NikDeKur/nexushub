/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.talker

import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.util.CloseCode
import java.util.function.Predicate

open class NOOPTalker(
    override val address: Address = Address("test", 0),
    override val isOpen: Boolean = true,
    override val isBlocked: Boolean = false
) : Talker {


    override suspend fun <R> send(
        packet: Packet,
        reaction: PacketReaction.Builder<R>.() -> Unit,
        respondTo: UShort?
    ): PacketTransmission<R> {
        throw NotImplementedError()
    }

    override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
        return null
    }

    override suspend fun close(code: CloseCode, comment: String) {
        throw NotImplementedError()
    }

    override suspend fun closeWithBlock(code: CloseCode, reason: String) {
        close(code, reason)
    }

    override suspend fun <T : Packet> wait(packetClass: Class<T>, condition: Predicate<T>): IncomingContext<T> {
        throw NotImplementedError()
    }
}