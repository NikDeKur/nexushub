/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.protection.argon2

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.protection.Password
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.service.NexusHubService

class Argon2ProtectionService(
    override val app: NexusHubServer
) : NexusHubService, ProtectionService {

    override fun averageEncryptionTime(): Int {
        return PasswordEncryptor.averageHashTime()
    }

    override fun createPassword(string: String): Password {
        return PasswordEncryptor.encryptNew(string)
    }

    override fun deserializePassword(string: String): Password {
        val parts = string.split(":")
        val bytes = parts[0].fromHEX()
        val saltByte = parts[1].fromHEX()
        val salt = Salt(saltByte)
        return Argon2Password(bytes, salt)
    }
}