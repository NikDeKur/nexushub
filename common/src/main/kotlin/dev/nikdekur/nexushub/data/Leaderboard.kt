/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.data


typealias Leaderboard = List<LeaderboardEntry>

class LeaderboardBuilder(size: Int) {
    var startFrom: Int = 0

    private inline val index: Long
        get() = (startFrom + entries.size).toLong()

    private val entries = ArrayList<LeaderboardEntry>(size)

    fun entry(holderId: String, value: Double) {
        entries += LeaderboardEntry(index, holderId, value)
    }

    fun build(): Leaderboard {
        return entries
    }
}

inline fun buildLeaderboard(size: Int = 10, block: LeaderboardBuilder.() -> Unit): Leaderboard {
    return LeaderboardBuilder(size).apply(block).build()
}