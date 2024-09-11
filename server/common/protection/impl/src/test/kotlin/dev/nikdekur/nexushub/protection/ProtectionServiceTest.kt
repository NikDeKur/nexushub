/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.protection

import dev.nikdekur.nexushub.protection.password.ProtectionService
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface ProtectionServiceTest {

    val service: ProtectionService

    @Test
    fun `test create password`() {
        service.createPassword("password")
    }

    @Test
    fun `test create and isEquals password`() {
        val password = service.createPassword("password")
        val equals = password.isEqual("password")
        assertTrue(equals)
    }

    @Test
    fun `test create and serialize password`() {
        val password = service.createPassword("password")
        password.serialize()
    }

    @Test
    fun `test create, serialize and deserialize password`() {
        val password = service.createPassword("password")
        val serialized = password.serialize()
        val deserialized = service.deserializePassword(serialized)

        assertEquals(password, deserialized)
    }
}