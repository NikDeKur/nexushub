/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.scope

import dev.nikdekur.ndkore.ext.*
import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.data.NexusData
import dev.nikdekur.nexushub.data.buildLeaderboard
import dev.nikdekur.nexushub.scope.data.ScopeData
import org.slf4j.LoggerFactory


class CachingScope(
    override val id: String,
    val data: ScopeData,
    val cache: MutableMap<String, NexusData>
) : Scope {

    val logger = LoggerFactory.getLogger(javaClass)


    override suspend fun loadData(holderId: String): NexusData {
        val cached = cache[holderId]
        if (cached != null) return cached
        val data = data.findOrNull(holderId) ?: emptyMap()
        cache.put(holderId, data)
        return data
    }


    override suspend fun setData(holderId: String, data: NexusData) {
        val clean = data.removeEmpty(maps = true, collections = true)
        cache.put(holderId, clean)
        this.data.save(holderId, clean)
    }


    override suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): Leaderboard {
        val leaderboard = logger.recordTiming(name = "getLeaderboard") {
            val rawLeaderboard = data.getLeaderboard(field, startFrom, limit)

            logger.info("Raw leaderboard: $rawLeaderboard")

            val pathList = field.split('.')

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

            val position = this.data.getTopPosition(holderId, field, value)

            LeaderboardEntry(position, holderId, value)
        }
        return position
    }
}