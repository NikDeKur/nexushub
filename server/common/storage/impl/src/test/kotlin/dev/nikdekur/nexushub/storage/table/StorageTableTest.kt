/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage.table

import dev.nikdekur.ndkore.test.assertEmpty
import dev.nikdekur.ndkore.test.assertSize
import dev.nikdekur.nexushub.storage.StorageTable
import dev.nikdekur.nexushub.storage.request.asc
import dev.nikdekur.nexushub.storage.request.desc
import dev.nikdekur.nexushub.storage.request.eq
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


data class TestObject(
    val field1: String,
    val field2: Int,
    val field3: Boolean
)

interface StorageTableTest {

    fun <T : Any> getTable(name: String, clazz: Class<T>): StorageTable<T>

    fun createTestData(index: Int = 0) = TestObject(
        field1 = "test-$index",
        field2 = index,
        field3 = index % 2 == 0
    )

    @Test
    fun `test getTable`() {
        assertDoesNotThrow {
            getTable<TestObject>("test")
        }
    }


    @Test
    fun `test insertOne`() = runTest {
        val table = getTable<TestObject>("test")
        table.insertOne(createTestData())
    }

    @Test
    fun `test insertOne and find`() = runTest {
        val table = getTable<TestObject>("test")
        val obj = createTestData()
        table.insertOne(obj)

        val data = table.find().toList()
        assertSize(data, 1)
        assertEquals(obj, data.first())
    }


    @Test
    fun `test insertOne few times`() = runTest {
        val table = getTable<TestObject>("test")
        repeat(5) {
            table.insertOne(createTestData(it))
        }
    }

    @Test
    fun `test insertOne few times and find`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)

        objects.forEach {
            table.insertOne(it)
        }

        val data = table.find().toList()

        assertSize(data, 5)
        assertContentEquals(objects, data)
    }


    @Test
    fun `test insertMany`() = runTest {
        val table = getTable<TestObject>("test")
        val list = List(3, ::createTestData)
        table.insertMany(list)
    }


    @Test
    fun `test insertMany and find`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(3, ::createTestData)
        table.insertMany(objects)

        val data = table.find().toList()
        assertSize(data, 3)
        assertContentEquals(objects, data)
    }

    @Test
    fun `test insertMany and find with limit`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(limit = 3).toList()
        assertSize(data, 3)
        assertContentEquals(objects.take(3), data)
    }


    @Test
    fun `test insertMany and find with skip`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(skip = 2).toList()
        assertSize(data, 3)
        assertContentEquals(objects.drop(2), data)
    }


    @Test
    fun `test insertMany and find with limit and skip`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(limit = 2, skip = 2).toList()
        assertSize(data, 2)
        assertContentEquals(objects.drop(2).take(2), data)
    }

    @Test
    fun `test insertMany and find with filters`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(listOf(TestObject::field2 eq 2)).toList()
        assertSize(data, 1)
        assertEquals(objects[2], data.first())
    }

    @Test
    fun `test insertMany and find with sort desc`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(sort = TestObject::field2.desc()).toList()
        assertSize(data, 5)
        assertContentEquals(objects.sortedByDescending { it.field2 }, data)
    }

    @Test
    fun `test insertMany and find with sort asc`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(sort = TestObject::field2.asc()).toList()
        assertSize(data, 5)
        assertContentEquals(objects.sortedBy { it.field2 }, data)
    }


    @Test
    fun `test insertMany and find with sort and limit`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(sort = TestObject::field2.desc(), limit = 2).toList()
        assertSize(data, 2)
        assertContentEquals(objects.sortedByDescending { it.field2 }.take(2), data)
    }


    @Test
    fun `test insertMany and find with sort and skip`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(sort = TestObject::field2.desc(), skip = 2).toList()
        assertSize(data, 3)
        assertContentEquals(objects.sortedByDescending { it.field2 }.drop(2), data)
    }


    @Test
    fun `test insertMany and find with sort, limit and skip`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(sort = TestObject::field2.desc(), limit = 2, skip = 2).toList()
        assertSize(data, 2)
        assertContentEquals(objects.sortedByDescending { it.field2 }.drop(2).take(2), data)
    }


    @Test
    fun `test insertMany and find with filters and sort`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(listOf(TestObject::field2 eq 2), TestObject::field2.desc()).toList()
        assertSize(data, 1)
        assertEquals(objects[2], data.first())
    }


    @Test
    fun `test insertMany and find with filters, sort, limit and skip`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(
            filters = listOf(TestObject::field2 eq 2),
            sort = TestObject::field2.desc(),
            limit = 1,
            skip = 0
        ).toList()
        assertSize(data, 1)
        assertEquals(objects[2], data.first())
    }


    @Test
    fun `test insertMany and find with filters, sort, limit and skip 1`() = runTest {
        val table = getTable<TestObject>("test")
        val objects = List(5, ::createTestData)
        table.insertMany(objects)

        val data = table.find(listOf(TestObject::field2 eq 2), TestObject::field2.desc(), 1, 1).toList()
        assertEmpty(data)
    }


    @Test
    fun `test count when no data`() = runTest {
        val table = getTable<TestObject>("test")
        val amount = table.count()
        assertEquals(0, amount)
    }


    @Test
    fun `test insert data and count`() = runTest {
        val table = getTable<TestObject>("test")
        table.insertOne(createTestData())
        val amount = table.count()
        assertEquals(1, amount)
    }


    @Test
    fun `test insert many data and count`() = runTest {
        val table = getTable<TestObject>("test")
        table.insertMany(List(3, ::createTestData))

        val amount = table.count()
        assertEquals(3, amount)
    }
}

inline fun <reified T : Any> StorageTableTest.getTable(name: String) = getTable(name, T::class.java)