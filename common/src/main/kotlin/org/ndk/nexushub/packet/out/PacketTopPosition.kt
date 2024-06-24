package org.ndk.nexushub.packet.out

import org.ndk.nexushub.data.LeaderboardEntry
import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes

/**
 * Packet for sending the top position of a leaderboard
 */
class PacketTopPosition : Packet {
    override val packetId = PacketTypes.TOP_POSITION.id

    /**
     * The leaderboard entry, storing the position and value
     *
     * No holderId inside to economise space
     *
     * If entry is null, no position is found
     */
    var entry: LeaderboardEntry? = null

    constructor() : super()
    constructor(entry: LeaderboardEntry?) : super() {
        this.entry = entry
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        val position = deserializer.readLong()
        val value = deserializer.readDouble()
        entry = if (position == -1L) {
            null
        } else {
            LeaderboardEntry(position, "", value)
        }
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeLong(entry?.position ?: -1L)
        serializer.writeDouble(entry?.value ?: -0.0)
    }

    override fun toString(): String {
        val e = entry ?: return "PacketTopPosition(No Position Found)"
        return "PacketTopPosition(position=${e.position}, value=${e.value})"
    }


}