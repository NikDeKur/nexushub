package org.ndk.nexushub.packet.out

import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes.REQUEST_SYNC

class PacketRequestSync : Packet {
    override val packetId = REQUEST_SYNC.id

    var scope: String = ""

    constructor()
    constructor(scope: String) {
        this.scope = scope
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scope = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scope)
    }


    override fun toString(): String {
        return "PacketRequestSync(scope='$scope')"
    }
}