package org.ndk.nexushub.packet

import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes.REQUEST_HOLDER_SYNC

class PacketRequestHolderSync : Packet {
    override val packetId = REQUEST_HOLDER_SYNC.id

    var scope: String = ""
    var holderId: String = ""

    constructor()
    constructor(scope: String, holderId: String) {
        this.scope = scope
        this.holderId = holderId
    }

    override fun deserialize(deserializer: org.ndk.nexushub.packet.serialize.PacketDeserializer) {
        scope = deserializer.readString()
        holderId = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scope)
        serializer.writeString(holderId)
    }


    override fun toString(): String {
        return "PacketRequestSync(scope='$scope')"
    }
}