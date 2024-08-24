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

/**
 * Packet sent by the server to the client
 * to indicate that authentication is successful and the client may start sending packets.
 *
 * @property heartbeatInterval The interval in milliseconds for the client to send heartbeat packets.
 */
class PacketReady : Packet {
    override val packetId = PacketTypes.READY.id

    constructor() : super()
    constructor(heartbeatInterval: Long) : super() {
        this.heartbeatInterval = heartbeatInterval
    }

    var heartbeatInterval: Long = 0

    override fun deserialize(deserializer: PacketDeserializer) {
        heartbeatInterval = deserializer.readLong()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeLong(heartbeatInterval)
    }

    override fun toString(): String {
        return "PacketReady(heartbeatInterval=$heartbeatInterval)"
    }
}