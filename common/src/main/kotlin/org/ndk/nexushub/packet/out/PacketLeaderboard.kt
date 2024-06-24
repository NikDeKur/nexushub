package org.ndk.nexushub.packet.out

import org.ndk.nexushub.data.Leaderboard
import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes

class PacketLeaderboard : Packet {
    override val packetId = PacketTypes.LEADERBOARD.id

    lateinit var leaderboard: Leaderboard

    /**
     * Additional entry for the requested position, if any
     *
     * Null if not requested or not found
     *
     * No [LeaderboardEntry.holderId] inside to economise space
     */
    var requestPosition: LeaderboardEntry? = null

    // Note:
    // - Sorted by top to bottom

    constructor() : super()
    constructor(leaderboard: Leaderboard, requestPosition: LeaderboardEntry?) : super() {
        this.leaderboard = leaderboard
        this.requestPosition = requestPosition
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        val size = deserializer.readUByte().toInt()
        leaderboard = Leaderboard(size)
        deserializer.readCollection(size) {
            val id = readString()
            val value = readDouble()
            leaderboard.addEntry(id, value)
        }

        if (deserializer.readBoolean()) {
            val pos = deserializer.readLong()
            val value = deserializer.readDouble()
            requestPosition = LeaderboardEntry(pos, "", value)
        }
    }

    override fun serialize(serializer: PacketSerializer) {
        val entries = leaderboard.entries
        val size = entries.size.toByte()
        serializer.writeList(entries, size) {
            writeString(it.holderId)
            writeDouble(it.value)
        }

        val entry = requestPosition
        serializer.writeBoolean(entry != null)
        if (entry != null) {
            serializer.writeLong(entry.position)
            serializer.writeDouble(entry.value)
        }
    }

    override fun toString(): String {
        return "PacketLeaderboard(leaderboard=$leaderboard, requestPosition='$requestPosition')"
    }
}