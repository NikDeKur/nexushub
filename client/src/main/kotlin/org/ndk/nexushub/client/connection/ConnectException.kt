package org.ndk.nexushub.client.connection

sealed class ConnectException(val comment: String) : Exception() {
    
    class NoResponseException(comment: String) : ConnectException(comment)
    class NoRuntimeResponseException(comment: String) : ConnectException(comment)
    class WrongCredentialsException(comment: String) : ConnectException(comment)
}