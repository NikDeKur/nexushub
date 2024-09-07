/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage

import dev.nikdekur.nexushub.storage.index.IndexOptions
import dev.nikdekur.nexushub.storage.request.Filter
import dev.nikdekur.nexushub.storage.request.Sort
import kotlinx.coroutines.flow.Flow

interface StorageTable<T : Any> {


    suspend fun insertOne(data: T)
    suspend fun insertMany(data: List<T>)

    suspend fun count(vararg filters: Filter): Long

    fun find(
        filters: List<Filter>? = null,
        sort: Sort? = null,
        limit: Int? = null,
        skip: Int? = null
    ): Flow<T>

    suspend fun replaceOne(data: T, vararg filters: Filter)

    suspend fun deleteOne(vararg filters: Filter)
    suspend fun deleteMany(vararg filters: Filter)


    suspend fun createIndex(name: String, keys: Map<String, Int>, options: IndexOptions)
}