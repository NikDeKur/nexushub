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
import dev.nikdekur.nexushub.packet.type.PacketTypes.SAVE_DATA


/**
 * (IN) Packet to save user data from the database
 *
 * Requires authentication to be processed
 */
open class PacketSaveData : Packet {

    override val packetId = SAVE_DATA.id

    var holderId: String = ""
    var scopeId: String = ""
    var data: String = "" // JSON

    constructor()
    constructor(scopeId: String, holderId: String, data: String) {
        this.holderId = holderId
        this.scopeId = scopeId
        this.data = data
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        holderId = deserializer.readString()
        scopeId = deserializer.readString()
        data = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(holderId)
        serializer.writeString(scopeId)
        serializer.writeString(data)
    }

    override fun toString(): String {
        return "PacketSaveData(holderId='$holderId', scopeId='$scopeId', data='$data')"
    }
}