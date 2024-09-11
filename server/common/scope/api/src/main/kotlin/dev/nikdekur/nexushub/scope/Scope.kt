/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.scope

import dev.nikdekur.ndkore.`interface`.Snowflake
import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.data.NexusData

interface Scope : Snowflake<String> {

    /**
     * Load the data for the given holder
     *
     * @param holderId holder to load the data for
     * @return the data for the given holder or empty data if not found
     */
    suspend fun loadData(holderId: String): NexusData

    /**
     * Set the data for the given holder
     *
     * Should be suspended until the data is saved
     *
     * @param holderId holder to set the data for
     * @param data data to set
     */
    suspend fun setData(holderId: String, data: NexusData)

    /**
     * Get the leaderboard for the given path
     *
     * @param field path to get the leaderboard for. Dots separate keys
     * @param startFrom start from the given position
     * @param limit limit the number of entries to get
     * @return the leaderboard for the given path
     */
    suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): Leaderboard

    /**
     * Get the top position in the leaderboard for the given field of the given holder
     *
     * @param field field to get the top position for
     * @param holderId holder to get the top position for
     * @return the top position in the leaderboard for the given field of the given holder (where 0 is max)
     * or null if not found
     * @throws NumberFormatException if the field is not a number
     */
    suspend fun getTopPosition(holderId: String, field: String): LeaderboardEntry?
}