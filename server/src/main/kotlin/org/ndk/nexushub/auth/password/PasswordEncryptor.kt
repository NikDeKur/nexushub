@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.auth.password

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom
import kotlin.random.Random


object PasswordEncryptor {


    const val SALT_SIZE = 16

    const val CASH_LENGTH = 64
    const val OPERATIONS = 4

    // 320 MB in KB terms
    const val PASSWORD_MEMORY = 320 * 1024

    const val AVERAGE_HASH_TIME_MS = 1200L
    const val AVERAGE_HASH_TIME_RANGE = 1200 / 100 * 15 // 15% range

    fun averageHashTime(): Long {
        return AVERAGE_HASH_TIME_MS + Random.nextInt(-AVERAGE_HASH_TIME_RANGE, AVERAGE_HASH_TIME_RANGE)
    }


    fun encrypt(password: String, salt: Salt): EncryptedPassword {
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
        return EncryptedPassword(result, salt)
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

@ExperimentalStdlibApi
inline fun ByteArray.toHEX(): String {
    return toHexString(HexFormat.Default)
}

@ExperimentalStdlibApi
inline fun String.fromHEX(): ByteArray {
    return hexToByteArray(HexFormat.Default)
}