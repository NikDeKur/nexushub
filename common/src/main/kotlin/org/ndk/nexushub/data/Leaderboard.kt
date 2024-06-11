package org.ndk.nexushub.data

/**
 * Leaderboard class stores the leaderboard entries
 *
 * @param size The default size of the leaderboard to allocate memory for future entries. Default is 10
 */
open class Leaderboard(size: Int = 10) {

    val entries = ArrayList<LeaderboardEntry>(size)

    fun addEntries(entries: List<LeaderboardEntry>) {
        this.entries.addAll(entries)
    }

    fun addEntry(entry: LeaderboardEntry) {
        entries.add(entry)
    }

    fun addEntry(holderId: String, value: Double) {
        val position = entries.size
        entries.add(LeaderboardEntry(position.toLong(), holderId, value))
    }

    fun isEmpty() = entries.isEmpty()

    fun clear() {
        entries.clear()
    }

    override fun toString(): String {
        return "Leaderboard(entries=$entries)"
    }

    companion object {
        val EMPTY = Leaderboard(0)
    }

}