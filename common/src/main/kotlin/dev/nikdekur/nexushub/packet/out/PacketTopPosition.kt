/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet.out

import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.serialize.PacketDeserializer
import dev.nikdekur.nexushub.packet.serialize.PacketSerializer
import dev.nikdekur.nexushub.packet.type.PacketTypes

/**
 * Packet for sending the top position of a leaderboard
 */
class PacketTopPosition : Packet {
    override val packetId = PacketTypes.TOP_POSITION.id

    /**
     * The leaderboard entry, storing the position, holderId, and value
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
        val holderId = deserializer.readString()
        val value = deserializer.readDouble()
        entry = if (position == -1L) {
            null
        } else {
            LeaderboardEntry(position, holderId, value)
        }
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeLong(entry?.position ?: -1L)
        serializer.writeString(entry?.holderId ?: "")
        serializer.writeDouble(entry?.value ?: -0.0)
    }

    override fun toString(): String {
        val e = entry ?: return "PacketTopPosition(No Position Found)"
        return "PacketTopPosition(position=${e.position}, value=${e.value})"
    }


}