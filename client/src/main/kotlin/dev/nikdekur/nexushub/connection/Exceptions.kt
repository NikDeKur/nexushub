/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.connection

sealed class ConnectException(description: String, serverComment: String) : Exception(
    "$description. Server: $serverComment"
) {

    class WrongCredentials(description: String, comment: String) : ConnectException(description, comment)
    class NodeAlreadyExists(description: String, comment: String) : ConnectException(description, comment)
    class TooManyConnections(description: String, comment: String) : ConnectException(description, comment)
    class InvalidData(description: String, comment: String) : ConnectException(description, comment)
    class AuthenticationTimeout(description: String, comment: String) : ConnectException(description, comment)
    class NodeIsNotAuthenticated(description: String, comment: String) : ConnectException(description, comment)

    class NoResponse(description: String) : NexusException(description, "No response from server")
}

sealed class NexusException(description: String, serverComment: String) : Exception(
    "$description. Server: $serverComment"
) {

    class NotConnected(description: String, comment: String) : NexusException(description, comment)
    class UnexpectedBehaviour(description: String, comment: String) : NexusException(description, comment)
    class NoRuntimeResponse(description: String, comment: String) : NexusException(description, comment)
}