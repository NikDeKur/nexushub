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
import dev.nikdekur.nexushub.packet.type.PacketTypes

class PacketReady : Packet {
    override val packetId = PacketTypes.READY.id

    constructor() : super()
    constructor(heartbeatInterval: Int) : super() {
        this.heartbeatInterval = heartbeatInterval
    }

    var heartbeatInterval: Int = 0

    override fun deserialize(deserializer: PacketDeserializer) {
        heartbeatInterval = deserializer.readInt()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeInt(heartbeatInterval)
    }

    override fun toString(): String {
        return "PacketReady(heartbeatInterval=$heartbeatInterval)"
    }
}