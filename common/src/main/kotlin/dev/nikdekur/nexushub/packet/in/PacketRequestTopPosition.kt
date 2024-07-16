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


class PacketRequestTopPosition : Packet {
    override val packetId = PacketTypes.REQUEST_TOP_POSITION.id

    var scopeId: String = ""
    var holderId: String = ""
    var field: String = ""

    constructor() : super()
    constructor(scope: String, holderId: String, field: String) : super() {
        this.scopeId = scope
        this.holderId = holderId
        this.field = field
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        holderId = deserializer.readString()
        field = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeString(holderId)
        serializer.writeString(field)
    }

    override fun toString(): String {
        return "PacketRequestTopPosition(scope='$scopeId', holderId='$holderId', field='$field')"
    }
}