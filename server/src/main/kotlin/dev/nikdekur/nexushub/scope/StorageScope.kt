/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.scope

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import dev.nikdekur.ndkore.ext.*
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.data.buildLeaderboard
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.service.NexusHubComponent
import dev.nikdekur.nexushub.storage.scope.ScopeTable
import dev.nikdekur.nexushub.util.NexusData
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

data class StorageScope(
    override val app: NexusHubServer,
    override val id: String,
    val collection: ScopeTable
) : Scope, NexusHubComponent {

    val logger = LoggerFactory.getLogger(javaClass)

    val datasetService: DataSetService by inject()



    //              HolderId
    val cache: Cache<String, NexusData> by lazy {
        CacheBuilder.newBuilder()
            .apply {
                val dataset = datasetService.getDataSet().cache
                val cacheExpiration = dataset.cacheExpiration
                val cacheSize = dataset.cacheMaxSize
                expireAfterWrite(cacheExpiration, TimeUnit.SECONDS)
                expireAfterAccess(cacheExpiration, TimeUnit.SECONDS)
                maximumSize(cacheSize)
            }.build()
    }


    override suspend fun loadData(holderId: String): NexusData {
        // Don't use cache[holderId] because CacheBuilder doesn't support async loading
        val cached = cache.getIfPresent(holderId)
        if (cached != null) return cached
        val data = collection.findOrNull(holderId) ?: emptyMap()
        cache.put(holderId, data)
        return data
    }


    override suspend fun setData(holderId: String, data: NexusData) {
        val clean = data.removeEmpty(maps = true, collections = true)
        cache.put(holderId, clean)
        collection.save(holderId, clean)
    }


    override suspend fun getLeaderboard(path: String, startFrom: Int, limit: Int): Leaderboard {
        val leaderboard = logger.recordTiming(name = "getLeaderboard") {
            val rawLeaderboard = collection.getLeaderboard(path, startFrom, limit)

            logger.info("Raw leaderboard: $rawLeaderboard")

            val pathList = path.split('.')

            buildLeaderboard {
                this.startFrom = startFrom

                rawLeaderboard.forEachSafe(Exception::printStackTrace) {
                    val holderId = it["holderId"] as String

                    @Suppress("kotlin:S6611") // We know that the field is present

                    // Get the value from the nested path
                    var value: Any = it
                    pathList.forEach { pathPart ->
                        value = (value as? Map<*, *>)?.get(pathPart) ?: return@forEach
                    }
                    val number = value as? Number ?: return@forEachSafe

                    entry(holderId, number.toDouble())
                }
            }
        }

        return leaderboard
    }


    /**
     * Get the top position in the leaderboard for the given field of the given holder
     *
     * May throw [NumberFormatException] if the field is not a number
     *
     * @param field field to get the top position for
     * @param holderId holder to get the top position for
     * @return the top position in the leaderboard for the given field of the given holder (where 0 is max)
     * or null if not found
     */
    override suspend fun getTopPosition(holderId: String, field: String): LeaderboardEntry? {
        val position = logger.recordTiming(name = "getTopPosition") {

            val data = loadData(holderId)
            logger.info("Data is $data")
            val keys = field.split(".")
            val fieldValue = data.getNested(keys) ?: return null
            logger.info("Field Value: $fieldValue")
            if (fieldValue !is Number)
                throw NumberFormatException("Field $field is not a number")
            val value = fieldValue.toDouble()

            val position = collection.getTopPosition(holderId, field, value)

            LeaderboardEntry(position, holderId, value)
        }
        return position
    }
}