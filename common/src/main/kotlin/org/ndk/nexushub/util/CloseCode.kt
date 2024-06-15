package org.ndk.nexushub.util

import org.ndk.nexushub.network.talker.Talker

enum class CloseCode {
    WRONG_CREDENTIALS,
    INVALID_DATA,
    AUTHENTICATION_TIMEOUT,
    NODE_IS_NOT_AUTHENTICATED,
    TOO_MANY_CONNECTIONS,
    UNEXPECTED_BEHAVIOUR,

    ;

    val code = (4000 + ordinal).toShort()
}

suspend inline fun Talker.close(code: CloseCode, comment: String) {
    close(code.code, comment)
}