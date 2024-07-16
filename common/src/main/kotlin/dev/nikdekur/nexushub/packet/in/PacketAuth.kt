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
 * (IN) Packet for authentication
 *
 * All clients must send this packet first to authenticate.
 */
class PacketAuth : Packet {

    override val packetId = PacketTypes.AUTH.id

    var login: String = ""
    var password: String = ""
    var node: String = ""

    constructor()
    constructor(login: String, password: String, node: String) {
        this.login = login
        this.password = password
        this.node = node
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        login = deserializer.readString()
        password = deserializer.readString()
        node = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(login)
        serializer.writeString(password)
        serializer.writeString(node)
    }

    override fun toString(): String {
        return "PacketAuth(login='$login', password='$password', node='$node')"
    }
}
