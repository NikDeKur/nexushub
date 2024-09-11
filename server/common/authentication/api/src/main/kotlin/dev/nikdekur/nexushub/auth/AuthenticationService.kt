/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth

import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.node.Node

fun interface AuthenticationService {

    suspend fun authenticate(talker: Talker, credentials: Credentials): AuthResult

    sealed interface AuthResult {
        object AccountNotFound : AuthResult
        object WrongCredentials : AuthResult
        object NodeNameInvalid : AuthResult
        object NodeAtAddressAlreadyExists : AuthResult
        object NodeAlreadyExists : AuthResult
        class Success(val node: Node) : AuthResult
    }
}