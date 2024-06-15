package org.ndk.nexushub.client.connection

sealed class ConnectException(description: String, serverComment: String) : Exception (
    "$description. Server: $serverComment"
) {

    class WrongCredentials(description: String, comment: String) : ConnectException(description, comment)
    class TooManyConnections(description: String, comment: String) : ConnectException(description, comment)
    class InvalidData(description: String, comment: String) : ConnectException(description, comment)
    class AuthenticationTimeout(description: String, comment: String) : ConnectException(description, comment)
    class NodeIsNotAuthenticated(description: String, comment: String) : ConnectException(description, comment)

    class NoResponse(description: String) : NexusException(description, "No response from server")
}

sealed class NexusException(description: String, serverComment: String) : Exception (
    "$description. Server: $serverComment"
)  {

    class UnexpectedBehaviour(description: String, comment: String) : NexusException(description, comment)
    class NoRuntimeResponse(description: String, comment: String) : NexusException(description, comment)
}