/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull


@Serializable
data class TestStruct(
    val key1: String,
    val key2: Int,
    val key3: Boolean,
    val key4: Double
)

@Serializable
data class NestedStruct(
    val structs: List<TestStruct>
)

interface DataSetServiceTest {

    val dataset: DataSetService

    @Test
    fun `test string get`() {
        val string = dataset.get<String>("key1")
        assertEquals("value1", string)
    }

    @Test
    fun `test int get`() {
        val int = dataset.get<Int>("key2")
        assertEquals(2, int)
    }

    @Test
    fun `test boolean get`() {
        val boolean = dataset.get<Boolean>("key3")
        assertEquals(true, boolean)
    }

    @Test
    fun `test double get`() {
        val double = dataset.get<Double>("key4")
        assertEquals(4.0, double)
    }

    @Test
    fun `test non-existing get`() {
        val string = dataset.get<String>("non-existing")
        assertNull(string)
    }


    @Test
    fun `test struct get`() {
        val struct = dataset.get<TestStruct>("key5")
        assertEquals(
            TestStruct("value1", 2, true, 4.0),
            struct
        )
    }


    @Test
    fun `test nested struct get`() {
        val struct = dataset.get<NestedStruct>("key6")
        assertNotNull(struct)

        assertEquals(
            NestedStruct(
                listOf(
                    TestStruct("value1", 2, true, 4.0),
                    TestStruct("value2", 3, false, 5.0)
                )
            ),
            struct
        )
    }

}
