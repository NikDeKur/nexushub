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