package org.ndk.nexushub.packet.out

import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes

class PacketStopSession : Packet {
    override val packetId = PacketTypes.STOP_SESSION.id

    var scopeId: String = ""
    var holderId: String = ""

    constructor() : super()
    constructor(scopeId: String, holderId: String) : super() {
        this.scopeId = scopeId
        this.holderId = holderId
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        holderId = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeString(holderId)
    }

    override fun toString(): String {
        return "PacketStopSession(scopeId='$scopeId', holderId='$holderId')"
    }
}