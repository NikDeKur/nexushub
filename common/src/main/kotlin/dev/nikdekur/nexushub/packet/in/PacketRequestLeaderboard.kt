/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet.`in`

import dev.nikdekur.ndkore.ext.buildRepresentation
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.serialize.PacketDeserializer
import dev.nikdekur.nexushub.packet.serialize.PacketSerializer
import dev.nikdekur.nexushub.packet.type.PacketTypes

class PacketRequestLeaderboard : Packet {

    override val packetId = PacketTypes.REQUEST_LEADERBOARD.id

    var scopeId: String = ""
    var field: String = ""
    var startFrom: Int = 0
    var limit: Int = 0

    var requestPosition: String = ""

    constructor() : super()
    constructor(scopeId: String, filter: String, startFrom: Int, limit: Int, requestPosition: String) : super() {
        this.scopeId = scopeId
        this.field = filter
        this.startFrom = startFrom
        this.limit = limit
        this.requestPosition = requestPosition
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        field = deserializer.readString()
        startFrom = deserializer.readInt()
        limit = deserializer.readInt()
        requestPosition = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeString(field)
        serializer.writeInt(startFrom)
        serializer.writeInt(limit)
        serializer.writeString(requestPosition)
    }

    override fun toString(): String {
        return buildRepresentation(this)
    }
}