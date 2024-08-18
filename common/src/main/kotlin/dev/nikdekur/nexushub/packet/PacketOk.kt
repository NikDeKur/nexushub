/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet

import dev.nikdekur.nexushub.packet.serialize.PacketDeserializer
import dev.nikdekur.nexushub.packet.serialize.PacketSerializer
import dev.nikdekur.nexushub.packet.type.PacketTypes.OK


class PacketOk : Packet {

    override val packetId = OK.id

    var message: String = ""

    constructor()
    constructor(message: String) {
        this.message = message
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        message = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(message)
    }

    override fun toString(): String {
        return "PacketOk(message='$message')"
    }

}