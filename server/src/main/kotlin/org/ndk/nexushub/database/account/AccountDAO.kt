@file:OptIn(ExperimentalStdlibApi::class)

package org.ndk.nexushub.database.account

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import org.ndk.nexushub.auth.account.Account
import org.ndk.nexushub.auth.password.EncryptedPassword
import org.ndk.nexushub.auth.password.Salt
import org.ndk.nexushub.auth.password.fromHEX

data class AccountDAO(
    @BsonId
    val id: ObjectId = ObjectId(),
    val login: String,
    val password: String,
    val salt: String,
    val allowedScopes: Set<String>,
) {


    fun toHighLevel(): Account {
        val saltBytes = salt.fromHEX()
        val salt = Salt(saltBytes)

        val passwordBytes = password.fromHEX()
        val password = EncryptedPassword(passwordBytes, salt)

        return Account(login, password, allowedScopes)
    }
}