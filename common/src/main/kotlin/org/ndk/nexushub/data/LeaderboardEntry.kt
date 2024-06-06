package org.ndk.nexushub.data

/**
 * Leaderboard entry class
 *
 * @param position The position of the entry (starts from 1)
 * @param holderId The holder id of the entry
 * @param value The value of the entry
 */
data class LeaderboardEntry(
    val position: Long,
    val holderId: String,
    val value: Double
)