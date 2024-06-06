package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes.PONG

class PacketPong : Packet() {
    override val packetId = PONG.id


    override fun deserialize(deserializer: PacketDeserializer) {
    }

    override fun serialize(serializer: PacketSerializer) {
    }

    override fun toString(): String {
        return "PacketPong()"
    }
}