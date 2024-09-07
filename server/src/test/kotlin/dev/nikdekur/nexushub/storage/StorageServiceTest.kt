/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage

import dev.nikdekur.nexushub.scope.assertEmpty
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

interface StorageServiceTest {

    val service: StorageService


    @Test
    fun `test tables when no tables exist`() = runTest {
        val tables = service.getAllTables().toList()
        assertEmpty(tables)
    }


    @Test
    fun `test get table`() = runTest {
        assertDoesNotThrow {
            service.getTable<Any>("test")
        }
    }
}