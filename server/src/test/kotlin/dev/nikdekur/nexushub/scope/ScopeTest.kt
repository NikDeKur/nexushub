/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.scope

import dev.nikdekur.nexushub.util.NexusData
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Suppress("NOTHING_TO_INLINE")
inline fun NexusData.compare(data: NexusData) {
    for ((key, value) in data) {
        assertEquals(value, this[key])
    }
}

interface ScopeTest {

    suspend fun getScope(scopeId: String): Scope

    @Test
    fun `test create scope`(): Unit = runBlocking {
        getScope("test_scope")
    }


    @Test
    fun `test load empty data from scope`() = runBlocking {
        val scope = getScope("test_scope")
        val data = scope.loadData("test_holder")
        assertEmpty(data)
    }


    fun createTestData(num: Int = 1): NexusData = mapOf(
        "test" to num,
        "test_num" to "test$num"
    )

    @Test
    fun `test save data to scope`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        val data = createTestData()

        scope.setData("test_holder", data)
    }


    @Test
    fun `test save data to scope and load data`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        val data = createTestData()

        scope.setData("test_holder", data)

        val loadedData = scope.loadData("test_holder")
        assertSize(loadedData, 2)

        data.compare(loadedData)
    }

    @Test
    fun `test save data to scope and load data with different holder`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        val data = createTestData()

        scope.setData("test_holder", data)

        val loadedData = scope.loadData("test_holder_2")
        assertEmpty(loadedData)
    }

    @Test
    fun `test multiple save and load data`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        val data = createTestData()

        scope.setData("test_holder", data)

        val loadedData = scope.loadData("test_holder")
        assertSize(loadedData, 2)

        data.compare(loadedData)

        val data2 = createTestData(2)
        scope.setData("test_holder", data2)

        val loadedData2 = scope.loadData("test_holder")
        assertSize(loadedData2, 2)

        data2.compare(loadedData2)
    }

    @Test
    fun `test multiple holders save and load data`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        val data = createTestData()

        scope.setData("test_holder", data)

        val loadedData = scope.loadData("test_holder")
        assertSize(loadedData, 2)

        data.compare(loadedData)

        val data2 = createTestData(2)
        scope.setData("test_holder_2", data2)

        val loadedData2 = scope.loadData("test_holder_2")
        assertSize(loadedData2, 2)

        data2.compare(loadedData2)
    }


    @Test
    fun `test get empty leaderboard`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        val leaderboard = scope.getLeaderboard("test", 0, 10)
        assertEmpty(leaderboard)
    }

    @Test
    fun `test get one entry leaderboard`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        val data = createTestData()
        scope.setData("test_holder", data)

        val leaderboard = scope.getLeaderboard("test", 0, 10)
        assertSize(leaderboard, 1)
    }

    @Test
    fun `test get many entries leaderboard`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        scope.setData("test_holder", createTestData(1))
        scope.setData("test_holder_2", createTestData(2))

        val leaderboard = scope.getLeaderboard("test", 0, 10)
        assertSize(leaderboard, 2)
    }


    @Test
    fun `test get many entries leaderboard with limit`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        scope.setData("test_holder", createTestData(1))
        scope.setData("test_holder_2", createTestData(2))

        val leaderboard = scope.getLeaderboard("test", 0, 1)
        assertSize(leaderboard, 1)
    }


    @Test
    fun `test get many leaderboard entries with startFrom`(): Unit = runBlocking {
        val scope = getScope("test_scope")
        scope.setData("test_holder", createTestData(1))
        scope.setData("test_holder_2", createTestData(2))

        val leaderboard = scope.getLeaderboard("test", 1, 10)
        assertSize(leaderboard, 1)
    }
}