package org.ndk.nexushub.packet

import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes


class PacketRequestTopPosition : Packet {
    override val packetId = PacketTypes.REQUEST_TOP_POSITION.id

    var scopeId: String = ""
    var holderId: String = ""
    var field: String = ""

    constructor() : super()
    constructor(scope: String, holderId: String, field: String) : super() {
        this.scopeId = scope
        this.holderId = holderId
        this.field = field
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        holderId = deserializer.readString()
        field = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeString(holderId)
        serializer.writeString(field)
    }

    override fun toString(): String {
        return "PacketRequestTopPosition(scope='$scopeId', holderId='$holderId', field='$field')"
    }
}