/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.serial

import dev.nikdekur.nexushub.util.NexusData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

interface SerialTest {

    val service: SerialService

    fun assertDeepEquals(a: Any?, b: Any?, path: String = "") {
        when {
            a === b -> return
            a == null || b == null -> assertEquals(a, b, "Null mismatch at $path")
            a is Number && b is Number -> assertEquals(a.toDouble(), b.toDouble(), "Number mismatch at $path")
            a is List<*> && b is List<*> -> {
                assertEquals(a.size, b.size, "List size mismatch at $path")
                a.zip(b).forEachIndexed { index, (aElem, bElem) ->
                    assertDeepEquals(aElem, bElem, "$path[$index]")
                }
            }

            a is Collection<*> && b is Collection<*> -> {
                assertEquals(a.size, b.size, "Collection size mismatch at $path")
                a.forEach { aElem ->
                    assertTrue(
                        b.any { bElem -> deepEquals(aElem, bElem) },
                        "Collection element mismatch at $path: element <$aElem> not found in <$b>"
                    )
                }
            }

            a is Map<*, *> && b is Map<*, *> -> {
                assertEquals(a.size, b.size, "Map size mismatch at $path")
                a.keys.forEach { key ->
                    assertDeepEquals(a[key], b[key], "$path.$key")
                }
            }

            else -> assertEquals(a, b, "Value mismatch at $path")
        }
    }

    fun deepEquals(a: Any?, b: Any?): Boolean {
        return when {
            a === b -> true
            a == null || b == null -> false
            a is Number && b is Number -> a.toDouble() == b.toDouble()
            a is List<*> && b is List<*> -> a.size == b.size && a.zip(b)
                .all { (aElem, bElem) -> deepEquals(aElem, bElem) }

            a is Collection<*> && b is Collection<*> -> a.size == b.size && a.all { aElem ->
                b.any { bElem ->
                    deepEquals(
                        aElem,
                        bElem
                    )
                }
            }

            a is Map<*, *> && b is Map<*, *> -> a.size == b.size && a.keys.all { key -> deepEquals(a[key], b[key]) }
            else -> a == b
        }
    }

    fun go(data: NexusData) {
        val serialized = service.serialize(data)
        val deserialized = service.deserialize(serialized)
        assertDeepEquals(data, deserialized)
    }

    @Test
    fun `test simple serializing and deserializing`() {
        val data = mapOf(
            "key" to "value",
        )

        go(data)
    }

    @Test
    fun `test multitypes serializing and deserializing`() {
        val data = mapOf(
            "key" to "value",
            "key2" to 2,
            "key3" to true
        )

        go(data)
    }

    @Test
    fun `test nested serializing and deserializing`() {
        val data = mapOf(
            "key" to mapOf(
                "nested" to "value",
                "nested2" to 2,
            ),
        )

        go(data)
    }

    @Test
    fun `test list serializing and deserializing`() {
        val data = mapOf(
            "key" to listOf(
                "value",
                2,
                true
            ),
        )

        go(data)
    }

    @Test
    fun `test nested list serializing and deserializing`() {
        val data = mapOf(
            "key" to listOf(
                mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                ),
                mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                ),
            ),
        )

        go(data)
    }


    @Test
    fun `test nested multitypes serializing and deserializing`() {
        val data = mapOf(
            "key" to mapOf(
                "nested" to "value",
                "nested2" to 2,
                "nested3" to true
            ),
        )

        go(data)
    }

    @Test
    fun `test nested list multitypes serializing and deserializing`() {
        val data = mapOf(
            "key" to listOf(
                mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                    "nested3" to true
                ),
                mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                    "nested3" to true
                ),
            ),
        )

        go(data)
    }

    @Test
    fun `test random types nested serializing and deserializing`() {
        val data = mapOf(
            "key" to mapOf(
                "nested" to listOf(
                    "value",
                    2,
                    true
                ),
                "nested2" to mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                    "nested3" to true
                ),
            ),
            "key2" to listOf(
                mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                    "nested3" to true
                ),
                mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                    "nested3" to true
                ),
            ),
            "key3" to setOf(
                mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                    "nested3" to true
                ),
                mapOf(
                    "nested" to "value",
                    "nested2" to 2,
                    "nested3" to true
                ),
            )
        )

        go(data)
    }
}