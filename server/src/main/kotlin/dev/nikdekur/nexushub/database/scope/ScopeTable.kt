/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.database.scope

import dev.nikdekur.ndkore.`interface`.Snowflake
import dev.nikdekur.nexushub.util.NexusData

interface ScopeTable : Snowflake<String> {

    val data: ScopeDAO

    /**
     * Find the data for the given holder id
     *
     * @param holderId holder id
     * @return the data for the given holder id, or null if not found
     */
    suspend fun findOrNull(holderId: String): NexusData?

    /**
     * Save the data for the given holder id
     *
     * Do nothing if the existing data are same as the new data
     *
     * If old data exists and the new data is not empty, update the data
     *
     * If the new data is empty and old data exists, delete the data
     *
     * @param holderId holder id
     * @param data data to save
     */
    suspend fun save(holderId: String, data: NexusData)

    /**
     * Get the leaderboard for the given field, starting from the given position and limited by the given limit
     *
     * The leaderboard is sorted in descending order
     *
     * @param field field to get the leaderboard for
     * @param startFrom position to start from
     * @param limit limit of the leaderboard
     * @return the leaderboard for the given field
     */
    suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): List<NexusData>

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
    suspend fun getTopPosition(holderId: String, field: String, value: Double): Long
}