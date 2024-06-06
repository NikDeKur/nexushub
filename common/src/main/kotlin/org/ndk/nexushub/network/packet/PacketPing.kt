package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes.PING

class PacketPing : Packet() {

    override val packetId = PING.id

    override fun deserialize(deserializer: PacketDeserializer) {
        // No data transferred
    }

    override fun serialize(serializer: PacketSerializer) {
        // No data transferred
    }

    override fun toString(): String {
        return "PacketPing()"
    }
}