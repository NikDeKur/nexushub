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
) : Comparable<LeaderboardEntry> {

    override fun compareTo(other: LeaderboardEntry): Int {
        return position.compareTo(other.position)
    }

    companion object {
        val EMPTY = LeaderboardEntry(-1, "", 0.0)
    }
}