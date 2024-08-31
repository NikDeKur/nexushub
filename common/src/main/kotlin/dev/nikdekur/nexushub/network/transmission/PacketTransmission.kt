/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.transmission

import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.packet.Packet
import kotlin.time.Duration

interface PacketTransmission<R> {

    val talker: Talker
    val packet: Packet
    val reaction: PacketReaction<R>

    val respondTo: UShort?
    val received: Boolean


    suspend fun onTimeout(timeout: Duration)
    suspend fun onReceive(talker: Talker, packet: Packet)
    suspend fun onException(talker: Talker, e: Exception)

    suspend fun await(): R
}