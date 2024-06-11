package org.ndk.nexushub.packet

import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes.PING


class PacketPing : Packet() {

    override val packetId = PING.id

    override fun deserialize(deserializer: org.ndk.nexushub.packet.serialize.PacketDeserializer) {
        // No data transferred
    }

    override fun serialize(serializer: PacketSerializer) {
        // No data transferred
    }

    override fun toString(): String {
        return "PacketPing()"
    }
}