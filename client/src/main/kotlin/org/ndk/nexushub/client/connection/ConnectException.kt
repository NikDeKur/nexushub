package org.ndk.nexushub.client.connection

sealed class ConnectException(comment: String) : Exception(comment) {
    
    class NoResponseException(comment: String) : ConnectException(comment)
    class NoRuntimeResponseException(comment: String) : ConnectException(comment)
    class WrongCredentialsException(comment: String) : ConnectException(comment)
}