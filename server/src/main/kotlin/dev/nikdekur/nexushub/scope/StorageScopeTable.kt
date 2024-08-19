/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.scope

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.service.NexusHubComponent
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.StorageTable
import dev.nikdekur.nexushub.storage.index.indexOptions
import dev.nikdekur.nexushub.storage.request.Filter
import dev.nikdekur.nexushub.storage.request.Order
import dev.nikdekur.nexushub.storage.request.Sort
import dev.nikdekur.nexushub.storage.request.eq
import dev.nikdekur.nexushub.storage.request.gt
import dev.nikdekur.nexushub.storage.request.ne
import dev.nikdekur.nexushub.storage.scope.ScopeDAO
import dev.nikdekur.nexushub.storage.scope.ScopeTable
import dev.nikdekur.nexushub.util.NexusData
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.bson.Document
import org.slf4j.LoggerFactory

class StorageScopeTable(
    override val app: NexusHubServer,
    /**
     * The id of the scope
     */
    override val id: String,
    val table: StorageTable<NexusData>,
    override var data: ScopeDAO
) : ScopeTable, NexusHubComponent {

    val storage: StorageService by inject()
    val scopesService: ScopesService by inject()

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

        val dataDocument = Document("holderId", holderId).apply {
            data.forEach { (key, value) -> append(key, value) }
        }

        // If the old data does not exist, insert the new data
        if (old == null) {
            table.insertOne(dataDocument)
        } else {
            // If the old data exists, update the data
            table.replaceOne(dataDocument, idFilter(holderId))
        }
    }


    inline fun idFilter(holderId: String): Filter {
        return "holderId" eq holderId
    }


    override suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): List<NexusData> {
        ensureIndexAsync(field)
        return table
            .find(sort = Sort(field, Order.DESCENDING), limit = limit, skip = startFrom)
            .toList()
    }

    /**
     * Get the top position in the leaderboard for the given field of the given holder
     *
     * If no field is found for the holder or field is not a double, null is returned
     *
     * The top position is started from 0, so the top position is 0, the second position is 1, and so on
     *
     * @param holderId holder id
     * @param field field to get the top position for
     * @param value value to compare
     * @return the top position in the leaderboard for the given field of the given holder
     */
    override suspend fun getTopPosition(holderId: String, field: String, value: Double): Long {
        // Count the number of documents that have a value greater than the given value,
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


    private inline fun ensureIndexAsync(field: String): Job? {
        if (data.indexes.contains(field)) return null

        return storage.scope.launch {
            // Update scope in another coroutine to avoid blocking
            data = data.copy(indexes = data.indexes + field)
            createIndex(field, false)
            scopesService.updateScopeData(data)
        }
    }

    suspend fun createIndex(field: String, unique: Boolean) {
        table.createIndex(field, mapOf("holderId" to 1), indexOptions {
            this.unique = unique
        })
    }

}