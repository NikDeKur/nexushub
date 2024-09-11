/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.scope

import dev.nikdekur.ndkore.test.assertEmpty
import dev.nikdekur.ndkore.test.assertSize
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test


interface ScopesServiceTest {

    val service: ScopesService

    @Test
    fun `test get list of empty scopes`() = runTest {
        val scopes = service.getScopes()
        assertEmpty(scopes)
    }

    @Test
    fun `test create scope`() = runTest {
        service.getScope("test")
    }

    @Test
    fun `test create scope and get all scopes`() = runTest {
        service.getScope("test")
        val scopes = service.getScopes()
        assertSize(scopes, 1)
    }
}