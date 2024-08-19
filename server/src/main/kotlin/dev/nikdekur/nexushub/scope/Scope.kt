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
import dev.nikdekur.nexushub.util.NexusData

interface Scope : Snowflake<String> {

    suspend fun loadData(holderId: String): NexusData
    suspend fun setData(holderId: String, data: NexusData)
    suspend fun getLeaderboard(path: String, startFrom: Int, limit: Int): Leaderboard

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
    suspend fun getTopPosition(holderId: String, field: String): LeaderboardEntry?
}