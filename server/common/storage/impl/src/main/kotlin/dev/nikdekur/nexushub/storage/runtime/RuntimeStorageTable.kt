/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage.runtime

import dev.nikdekur.nexushub.storage.StorageTable
import dev.nikdekur.nexushub.storage.request.CompOperator
import dev.nikdekur.nexushub.storage.request.Filter
import dev.nikdekur.nexushub.storage.request.Order
import dev.nikdekur.nexushub.storage.request.Sort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlin.collections.get

class RuntimeStorageTable<T : Any> : StorageTable<T> {

    private val storage: MutableList<T> = mutableListOf()

    override suspend fun count(vararg filters: Filter): Long {
        return storage.asSequence()
            .filter { item -> filters.all { applyFilter(item, it) } }
            .count()
            .toLong()
    }

    override suspend fun insertOne(data: T) {
        storage.add(data)
    }

    override suspend fun insertMany(data: List<T>) {
        storage.addAll(data)
    }

    override suspend fun replaceOne(data: T, vararg filters: Filter) {
        val index = storage.indexOfFirst { item -> filters.all { applyFilter(item, it) } }
        if (index != -1) {
            storage[index] = data
        }
    }

    override fun find(
        filters: List<Filter>?,
        sort: Sort?,
        limit: Int?,
        skip: Int?
    ): Flow<T> = flow {
        val filteredData = storage.asSequence()
            .filter { item -> filters?.all { applyFilter(item, it) } != false }
            .let { sequence ->
                sort?.let { applySort(sequence, it) } ?: sequence
            }
            .let { sequence ->
                sequence.drop(skip ?: 0).take(limit ?: sequence.count())
            }
            .toList()
        emitAll(filteredData.asFlow())
    }

    override suspend fun deleteOne(vararg filters: Filter) {
        val index = storage.indexOfFirst { item -> filters.all { applyFilter(item, it) } }
        if (index != -1) {
            storage.removeAt(index)
        }
    }

    override suspend fun deleteMany(vararg filters: Filter) {
        storage.removeAll { item -> filters.all { applyFilter(item, it) } }
    }

    override suspend fun createIndex(
        name: String,
        keys: Map<String, Int>,
        options: dev.nikdekur.nexushub.storage.index.IndexOptions
    ) {
        // Индексация данных в оперативной памяти может быть опциональной
        // или базироваться на сортировке и использовании вспомогательных структур
    }

    private fun applyFilter(item: T, filter: Filter): Boolean {
        val value = item::class.members
            .firstOrNull { it.name == filter.key }
            ?.call(item)

        @Suppress("UNCHECKED_CAST")
        return when (filter.operator) {
            CompOperator.EQUALS -> value == filter.value
            CompOperator.NOT_EQUALS -> value != filter.value
            CompOperator.GREATER_THAN -> (value as Comparable<Any>) > filter.value
            CompOperator.LESS_THAN -> (value as Comparable<Any>) < filter.value
            CompOperator.GREATER_THAN_OR_EQUALS -> (value as Comparable<Any>) >= filter.value
            CompOperator.LESS_THAN_OR_EQUALS -> (value as Comparable<Any>) <= filter.value
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun findField(any: Any, field: String): Comparable<Any>? {
        return (if (any is Map<*, *>)
            any[field]
        else
            any::class.members.firstOrNull { it.name == field }?.call(any)
                ) as? Comparable<Any>
    }

    private fun applySort(sequence: Sequence<T>, sort: Sort): Sequence<T> {
        val comparator = Comparator<T> { a, b ->
            val field = sort.field
            val valueA = findField(a, field)
            val valueB = findField(b, field)
            if (valueA == null || valueB == null) return@Comparator 0
            when (sort.order) {
                Order.ASCENDING -> valueA.compareTo(valueB)
                Order.DESCENDING -> valueB.compareTo(valueA)
            }
        }
        return sequence.sortedWith(comparator)
    }
}
