@file:OptIn(ExperimentalStdlibApi::class)

package org.ndk.nexushub

import org.junit.Test
import org.ndk.nexushub.auth.password.EncryptedPassword
import org.ndk.nexushub.auth.password.PasswordEncryptor
import org.ndk.nexushub.auth.password.Salt
import org.ndk.nexushub.auth.password.fromHEX

class PasswordEncryptingTest {

    val userSalt = PasswordEncryptor.newSalt()

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testSaltHEXToByteConversion() {
        val saltHEX = userSalt.hex
        println("Salt HEX: $saltHEX")
        val saltByte = saltHEX.fromHEX()
        assert(userSalt.byte.contentEquals(Salt(saltByte).byte))
    }

    @Test
    fun testFullPasswordEncrypting() {
        val password = "Nikita08"
        val encryptedPassword = PasswordEncryptor.encrypt(password, userSalt)
        println("Encrypted password: ${encryptedPassword.byte.toList()}")

        val passwordHEX = encryptedPassword.hex
        val saltHEX = encryptedPassword.salt.hex

        val password2Byte = passwordHEX.fromHEX()
        val salt2Byte = saltHEX.fromHEX()

        val password2 = EncryptedPassword(password2Byte, Salt(salt2Byte))

        assert(encryptedPassword == password2)
    }
}