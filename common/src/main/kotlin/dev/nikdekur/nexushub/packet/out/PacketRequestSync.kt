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
import dev.nikdekur.nexushub.packet.type.PacketTypes.REQUEST_SYNC

class PacketRequestSync : Packet.Scope {
    override val packetId = REQUEST_SYNC.id

    override var scopeId: String = ""

    constructor()
    constructor(scope: String) {
        this.scopeId = scope
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
    }


    override fun toString(): String {
        return "PacketRequestSync(scope='$scopeId')"
    }
}