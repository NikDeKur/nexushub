/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet.out

import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.serialize.PacketDeserializer
import dev.nikdekur.nexushub.packet.serialize.PacketSerializer
import dev.nikdekur.nexushub.packet.type.PacketTypes.USER_DATA


/**
 * (OUT) Packet to send requested user data from the database
 */
class PacketUserData : Packet.Session {

    override val packetId = USER_DATA.id

    override var holderId: String = ""
    override var scopeId: String = ""
    var data: String = ""

    constructor()
    constructor(userId: String, scopeId: String, data: String) {
        this.holderId = userId
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
        return "PacketUserData(userId='$holderId', scopeId='$scopeId', data='$data')"
    }
}