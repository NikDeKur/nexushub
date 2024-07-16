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

/**
 * From server to a client packet that mean server is ready to receive [PacketAuth]
 */
class PacketHello() : Packet() {

    override val packetId = PacketTypes.HELLO.id

    override fun deserialize(deserializer: PacketDeserializer) {
        // no data
    }

    override fun serialize(serializer: PacketSerializer) {
        // no data
    }

    override fun toString(): String {
        return "PacketHello()"
    }
}