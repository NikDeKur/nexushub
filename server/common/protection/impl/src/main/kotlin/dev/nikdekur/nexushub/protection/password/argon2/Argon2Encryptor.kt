/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.protection.password.argon2

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import kotlin.random.Random


object Argon2Encryptor {


    const val SALT_SIZE = 16

    const val CASH_LENGTH = 64
    const val OPERATIONS = 4

    // 320 MB in KB terms
    const val PASSWORD_MEMORY = 320 * 1024

    const val AVERAGE_HASH_TIME_MS = 1200
    const val AVERAGE_HASH_TIME_RANGE = AVERAGE_HASH_TIME_MS / 100 * 15 // 15% range

    fun averageHashTime(): Int {
        return AVERAGE_HASH_TIME_MS + Random.nextInt(-AVERAGE_HASH_TIME_RANGE, AVERAGE_HASH_TIME_RANGE)
    }

    fun encryptNew(password: String): Pair<ByteArray, Salt> {
        val salt = newSalt()
        return encrypt(password, salt) to salt
    }

    fun encrypt(password: String, salt: Salt): ByteArray {
        val builder = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13) // 19
            .withIterations(OPERATIONS)
            .withMemoryAsKB(PASSWORD_MEMORY)
            .withParallelism(1)
            .withSalt(salt.byte)
            .build()

        val argon2 = Argon2BytesGenerator()
        argon2.init(builder)

        val result = ByteArray(CASH_LENGTH)
        argon2.generateBytes(password.encodeToByteArray(), result, 0, result.size)
        return result
    }


    // Note: SecureRandom is slower nearly 10 times than Random, but it's more secure
    val random = SecureRandom()

    // SecureRandom (and Random) is not thread-safe, so we need to synchronise it
    @Synchronized
    fun newSalt(): Salt {
        val array = ByteArray(SALT_SIZE)
        random.nextBytes(array)
        return Salt(array)
    }
}

@OptIn(ExperimentalStdlibApi::class)
inline fun ByteArray.toHEX(): String {
    return toHexString(HexFormat.Default)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun String.fromHEX(): ByteArray {
    return hexToByteArray(HexFormat.Default)
}