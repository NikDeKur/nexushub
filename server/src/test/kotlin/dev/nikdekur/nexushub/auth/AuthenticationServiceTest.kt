/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth

import dev.nikdekur.nexushub.auth.AuthenticationService.AuthResult
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.talker.NOOPTalker
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * We guarantee that the [service] will be the new instance of [AuthenticationService] for each test.
 *
 * And we expect to have a way to authenticate with valid credentials ["login1", "password1"] and ["login2", "password2"].
 */
interface AuthenticationServiceTest {

    val service: AuthenticationService


    @Test
    fun `test authenticate with invalid login`() = runTest {
        // Given
        val credentials = Credentials("login0", "password0", "node1")

        val talker = NOOPTalker()

        // When
        val result = service.authenticate(talker, credentials)

        // Then
        assertEquals(AuthResult.AccountNotFound, result)
    }


    @Test
    fun `test authenticate with invalid password`() = runTest {
        // Given
        val credentials = Credentials("login1", "password0", "node1")

        val talker = NOOPTalker()

        // When
        val result = service.authenticate(talker, credentials)

        // Then
        assertEquals(AuthResult.WrongCredentials, result)
    }


    @Test
    fun `test authenticate with invalid node name`() = runTest {
        // Given
        val credentials = Credentials("login1", "password1", "&")

        val talker = NOOPTalker()

        // When
        val result = service.authenticate(talker, credentials)

        // Then
        assertEquals(AuthResult.NodeNameInvalid, result)
    }


    @Test
    fun `test authenticate with existing node at talker address`() = runTest {
        // Given
        val credentials = Credentials("login2", "password2", "node2")

        val talker = NOOPTalker()

        // When
        service.authenticate(talker, credentials)
        val result2 = service.authenticate(talker, credentials)

        // Then
        assertEquals(AuthResult.NodeAtAddressAlreadyExists, result2)
    }


    @Test
    fun `test authenticate with existing node`() = runTest {
        // Given
        val credentials = Credentials("login2", "password2", "node2")

        val talker = NOOPTalker()
        val talker2 = NOOPTalker(Address("test", 1))

        // When
        service.authenticate(talker, credentials)
        val result2 = service.authenticate(talker2, credentials)

        // Then
        assertEquals(AuthResult.NodeAlreadyExists, result2)
    }


    @Test
    fun `test authenticate with valid credentials`() = runTest {
        // Given
        val credentials = Credentials("login1", "password1", "node1")

        val talker = NOOPTalker()

        // When
        val result = service.authenticate(talker, credentials)

        // Then
        assertIs<AuthResult.Success>(result)
    }
}