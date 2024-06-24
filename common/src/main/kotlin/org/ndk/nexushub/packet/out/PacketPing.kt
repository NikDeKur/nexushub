package org.ndk.nexushub.packet.out

import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketType
import org.ndk.nexushub.packet.type.PacketTypes

class PacketPing : Packet() {
    override val packetId = PacketTypes.PING.id

    override fun deserialize(deserializer: PacketDeserializer) {
        // No data
    }

    override fun serialize(serializer: PacketSerializer) {
        // No data
    }

    override fun toString(): String {
        return "PacketPing()"
    }
}