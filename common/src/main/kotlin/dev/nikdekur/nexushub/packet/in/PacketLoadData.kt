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
import dev.nikdekur.nexushub.packet.type.PacketTypes.LOAD_DATA

open class PacketLoadData : Packet {


    override val packetId = LOAD_DATA.id


    var holderId: String = ""
    var scopeId: String = ""

    constructor()
    constructor(scopeId: String, userId: String) {
        this.scopeId = scopeId
        this.holderId = userId
    }


    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        holderId = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeString(holderId)
    }

    override fun toString(): String {
        return "PacketLoadData(scopeId='$scopeId', holderId='$holderId')"
    }
}