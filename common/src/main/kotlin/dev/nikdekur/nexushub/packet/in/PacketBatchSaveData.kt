/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet.`in`

import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.serialize.PacketDeserializer
import dev.nikdekur.nexushub.packet.serialize.PacketSerializer
import dev.nikdekur.nexushub.packet.type.PacketTypes

class PacketBatchSaveData : Packet {
    override val packetId = PacketTypes.BATCH_SAVE_DATA.id

    var scopeId: String = ""

    /**
     * HolderId to JSON Data
     */
    var data: Map<String, String> = emptyMap()

    constructor()
    constructor(scopeId: String, data: Map<String, String>) {
        this.scopeId = scopeId
        this.data = data
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        val size = deserializer.readInt()
        data = deserializer.readHashMap(
            size = size,
            keyReader = { readString() },
            valueReader = { readString() },
        )
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeMap(data,
            keyWriter = { writeString(it) },
            valueWriter = { writeString(it) }
        )
    }

    override fun toString(): String {
        return "PacketBatchSaveData(scopeId='$scopeId', data=$data)"
    }
}