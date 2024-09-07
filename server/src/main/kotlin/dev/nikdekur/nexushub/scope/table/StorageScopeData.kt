/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.scope.table

import dev.nikdekur.nexushub.scope.StorageScopesService
import dev.nikdekur.nexushub.storage.StorageTable
import dev.nikdekur.nexushub.storage.index.indexOptions
import dev.nikdekur.nexushub.storage.request.Filter
import dev.nikdekur.nexushub.storage.request.Order
import dev.nikdekur.nexushub.storage.request.Sort
import dev.nikdekur.nexushub.storage.request.eq
import dev.nikdekur.nexushub.storage.request.gt
import dev.nikdekur.nexushub.storage.request.ne
import dev.nikdekur.nexushub.storage.scope.ScopeDAO
import dev.nikdekur.nexushub.util.NexusData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

data class StorageScopeData(
    val service: StorageScopesService,
    val scope: CoroutineScope,
    /**
     * The id of the scope
     */
    val id: String,
    val table: StorageTable<NexusData>,
    var data: ScopeDAO
) : ScopeData {

    val logger = LoggerFactory.getLogger("ScopeCollection")

    override suspend fun findOrNull(holderId: String): NexusData? {
        val filter = idFilter(holderId)
        return table.find(listOf(filter))
            .singleOrNull()
            ?.let {
                HashMap(it)
                    .apply {
                        remove("_id")
                        remove("holderId")
                    }
            }
    }


    override suspend fun save(holderId: String, data: NexusData) {
        val old = findOrNull(holderId)

        // If the existing data are same as the new data, return
        if (old == data) return

        // If the new data is empty and old data exists, delete the data
        if (old != null && data.isEmpty()) {
            table.deleteOne(idFilter(holderId))
            return
        }

        val data = LinkedHashMap(data)
            .also {
                it["holderId"] = holderId
            }

        // If the old data does not exist, insert the new data
        if (old == null) {
            table.insertOne(data)
        } else {
            // If the old data exists, update the data
            table.replaceOne(data, idFilter(holderId))
        }
    }


    inline fun idFilter(holderId: String): Filter {
        return "holderId" eq holderId
    }


    override suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): List<NexusData> {

        ensureIndexAsync(field)
        return table
            .find(
                sort = Sort(field, Order.DESCENDING),
                limit = limit,
                skip = startFrom
            )
            .toList()
    }

    override suspend fun getTopPosition(holderId: String, field: String, value: Double): Long {
        // Count the number of data`s that has a value greater than the given value,
        // And the holderId is not the given holderId (to exclude the given holderId)
        logger.info("[$id] Getting top position for $holderId with $field > $value")
        ensureIndexAsync(field)
        val filters = arrayOf(
            field gt value,
            "holderId" ne holderId
        )
        val count = table.count(*filters)
        return count
    }


    inline fun ensureIndexAsync(field: String) {
        scope.launch {
            if (data.indexes.contains(field)) return@launch

            // Update scope in another coroutine to avoid blocking
            data = data.copy(indexes = data.indexes + field)
            createIndex(field, false)
            service.updateScopeData(data)
        }
    }

    suspend fun createIndex(field: String, unique: Boolean) {
        table.createIndex(field, mapOf("holderId" to 1), indexOptions {
            this.unique = unique
        })
    }

}