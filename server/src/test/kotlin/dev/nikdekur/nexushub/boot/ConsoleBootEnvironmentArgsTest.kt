/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.boot

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull


class ConsoleBootEnvironmentArgsTest {

    @Test
    fun `test parsing single arg`() {
        val args = arrayOf("key1=value1")
        val environment = ConsoleBootEnvironment.fromCommandLineArgs(args)
        assertEquals("value1", environment.getValue("key1"))
    }

    @Test
    fun `test parsing multiple args`() {
        val args = arrayOf("key1=value", "key2=value2")
        val environment = ConsoleBootEnvironment.fromCommandLineArgs(args)
        assertEquals("value", environment.getValue("key1"))
        assertEquals("value2", environment.getValue("key2"))
    }

    @Test
    fun `test parsing empty args`() {
        val environment = ConsoleBootEnvironment.fromCommandLineArgs(emptyArray())
        assertNull(environment.getValue("key1"))
    }

    @Test
    fun `test returning null on missing arg`() {
        val args = arrayOf("key1=value1")
        val environment = ConsoleBootEnvironment.fromCommandLineArgs(args)
        assertNull(environment.getValue("key2"))
    }

    @Test
    fun `test throwing exception on invalid arg`() {
        val args = arrayOf("key1")
        assertThrows<IllegalArgumentException> {
            ConsoleBootEnvironment.fromCommandLineArgs(args)
        }
    }
}