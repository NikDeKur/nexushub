/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.protection.password.argon2

import dev.nikdekur.ndkore.ext.delay
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.protection.password.Password
import dev.nikdekur.nexushub.protection.password.ProtectionService
import dev.nikdekur.nexushub.service.NexusHubService
import kotlinx.coroutines.Dispatchers

class Argon2ProtectionService(
    override val app: NexusHubServer
) : NexusHubService(), ProtectionService {

    val encryptingDispatcher = Dispatchers.Default

    override fun createPassword(string: String): Argon2Password {
        val data = Argon2Encryptor.encryptNew(string)
        return Argon2Password(encryptingDispatcher, data.first, data.second)
    }

    override fun deserializePassword(string: String): Password {
        val parts = string.split(":")
        val bytes = parts[0].fromHEX()
        val saltByte = parts[1].fromHEX()
        val salt = Salt(saltByte)
        return Argon2Password(encryptingDispatcher, bytes, salt)
    }

    override suspend fun imitatePasswordEncryption() {
        val averageTime = Argon2Encryptor.averageHashTime()
        delay(averageTime)
    }
}