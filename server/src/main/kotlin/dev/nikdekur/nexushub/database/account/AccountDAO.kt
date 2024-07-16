@file:OptIn(ExperimentalStdlibApi::class)

package dev.nikdekur.nexushub.database.account

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import dev.nikdekur.nexushub.auth.account.Account
import dev.nikdekur.nexushub.auth.password.EncryptedPassword
import dev.nikdekur.nexushub.auth.password.Salt
import dev.nikdekur.nexushub.auth.password.fromHEX

@Serializable
data class AccountDAO(
    @BsonId @Transient val id: ObjectId = ObjectId(),
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

        return Account(this, login, password, allowedScopes.toMutableSet())
    }
}