/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.data

import kotlinx.serialization.Serializable

/**
 * Leaderboard entry class
 *
 * @param position The position of the entry (starts from 1)
 * @param holderId The holder id of the entry
 * @param value The value of the entry
 */
@Serializable
data class LeaderboardEntry(
    val position: Long,
    val holderId: String,
    val value: Double
) : Comparable<LeaderboardEntry> {

    override fun compareTo(other: LeaderboardEntry): Int {
        return position.compareTo(other.position)
    }
}