/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:OptIn(ExperimentalStdlibApi::class)

package dev.nikdekur.nexushub.database.account

import dev.nikdekur.nexushub.auth.account.Account
import dev.nikdekur.nexushub.auth.password.EncryptedPassword
import dev.nikdekur.nexushub.auth.password.Salt
import dev.nikdekur.nexushub.auth.password.fromHEX
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class AccountDAO(
    @BsonId @Transient val id: ObjectId = ObjectId(),
    val login: String,
    val password: String,
    val salt: String,
    val allowedScopes: Set<String>
) {


    fun toHighLevel(): Account {
        val saltBytes = salt.fromHEX()
        val salt = Salt(saltBytes)

        val passwordBytes = password.fromHEX()
        val password = EncryptedPassword(passwordBytes, salt)

        return Account(this, login, password, allowedScopes.toMutableSet())
    }
}