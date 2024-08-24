/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth

import dev.nikdekur.nexushub.node.Node
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.talker.ClientTalker

interface AuthenticationService : NexusHubService {


    suspend fun authenticate(talker: ClientTalker, packet: PacketAuth): AuthResult

    sealed interface AuthResult {
        class Success(val node: Node) : AuthResult
        object AccountNotFound : AuthResult
        object WrongCredentials : AuthResult
        object NodeNameInvalid : AuthResult
        object NodeAtAddressAlreadyExists : AuthResult
        object NodeAlreadyExists : AuthResult
    }
}