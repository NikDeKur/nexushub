package org.ndk.nexushub.auth.account

import org.ndk.nexushub.auth.password.EncryptedPassword

data class Account(
    val login: String,
    val password: EncryptedPassword,
    val allowedScopes: Set<String>
)