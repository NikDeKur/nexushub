package org.ndk.nexushub.packet

import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes.PONG


class PacketPong : Packet() {
    override val packetId = PONG.id


    override fun deserialize(deserializer: org.ndk.nexushub.packet.serialize.PacketDeserializer) {
    }

    override fun serialize(serializer: PacketSerializer) {
    }

    override fun toString(): String {
        return "PacketPong()"
    }
}