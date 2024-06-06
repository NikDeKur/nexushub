package org.ndk.nexushub.network.packet

import org.ndk.nexushub.data.Leaderboard
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes

class PacketLeaderboard : Packet {
    override val packetId = PacketTypes.LEADERBOARD.id

    var leaderboard: Leaderboard = Leaderboard(0)

    // Note:
    // - Sorted by top to bottom

    constructor() : super()
    constructor(leaderboard: Leaderboard) : super() {
        this.leaderboard = leaderboard
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        val entries = deserializer.readList {
            val id = readString()
            val value = readDouble()
            LeaderboardEntry(it + 1L, id, value)
        }
        leaderboard.addEntries(entries)
    }

    override fun serialize(serializer: PacketSerializer) {
        val entries = leaderboard.entries
        serializer.writeList(entries) {
            writeString(it.holderId)
            writeDouble(it.value)
        }
    }

    override fun toString(): String {
        return "PacketLeaderboard(leaderboard=$leaderboard)"
    }
}