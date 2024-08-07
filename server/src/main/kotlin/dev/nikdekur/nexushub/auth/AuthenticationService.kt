/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth

import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.talker.ClientTalker

interface AuthenticationService {

    suspend fun executeAuthenticatedPacket(talker: ClientTalker, context: IncomingContext<Packet>)

    suspend fun processAuth(talker: ClientTalker, packet: PacketAuth)
}