package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes.LOAD_DATA

open class PacketLoadData : Packet {


    override val packetId = LOAD_DATA.id


    var holderId: String = ""
    var scopeId: String = ""

    constructor()
    constructor(scopeId: String, userId: String) {
        this.scopeId = scopeId
        this.holderId = userId
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
        return "PacketLoadData(scopeId='$scopeId', holderId='$holderId')"
    }
}