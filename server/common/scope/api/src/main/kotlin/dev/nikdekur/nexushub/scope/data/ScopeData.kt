/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.scope.data

import dev.nikdekur.nexushub.data.NexusData

interface ScopeData {

    /**
     * Find the data of the given holder
     *
     * @param holderId holder id
     * @return the data of the given holder, or null if the data doesn't exist
     */
    suspend fun findOrNull(holderId: String): NexusData?

    /**
     * Save the data of the given holder
     *
     * @param holderId holder id
     * @param data data to save
     */
    suspend fun save(holderId: String, data: NexusData)

    /**
     * Get the leaderboard for the given path
     *
     * @param field path to get the leaderboard for
     * @param startFrom start from the given position
     * @param limit limit the number of entries to get
     * @return the leaderboard for the given path
     */
    suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): List<NexusData>

    /**
     * Get the top position in the leaderboard for the given field of the given holder
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