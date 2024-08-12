/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet.out

import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.data.buildLeaderboard
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.serialize.PacketDeserializer
import dev.nikdekur.nexushub.packet.serialize.PacketSerializer
import dev.nikdekur.nexushub.packet.type.PacketTypes

class PacketLeaderboard : Packet {
    override val packetId = PacketTypes.LEADERBOARD.id

    var startFrom = -1
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
    constructor(startFrom: Int, leaderboard: Leaderboard, requestPosition: LeaderboardEntry?) : super() {
        this.startFrom = startFrom
        this.leaderboard = leaderboard
        this.requestPosition = requestPosition
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        var startFrom = deserializer.readInt().dec()

        val size = deserializer.readUByte().toInt()

        this.leaderboard = buildLeaderboard(size) {
            this.startFrom = startFrom

            deserializer.readCollection(size) {
                val id = readString()
                val value = readDouble()
                entry(id, value)
            }
        }

        if (deserializer.readBoolean()) {
            val pos = deserializer.readLong()
            val holderId = deserializer.readString()
            val value = deserializer.readDouble()
            requestPosition = LeaderboardEntry(pos, holderId, value)
        }
    }

    override fun serialize(serializer: PacketSerializer) {
        val entries = leaderboard
        val size = entries.size.toByte()

        serializer.writeInt(startFrom)
        serializer.writeList(entries, size) {
            writeString(it.holderId)
            writeDouble(it.value)
        }

        val entry = requestPosition
        val writeEntry = entry != null
        serializer.writeBoolean(writeEntry)
        if (writeEntry) {
            serializer.writeLong(entry.position)
            serializer.writeString(entry.holderId)
            serializer.writeDouble(entry.value)
        }
    }

    override fun toString(): String {
        return "PacketLeaderboard(leaderboard=$leaderboard, requestPosition='$requestPosition')"
    }
}