package org.ndk.nexushub.packet

import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes.OK


class PacketOk : Packet {

    override val packetId = OK.id

    var message: String = ""
    constructor()
    constructor(message: String) {
        this.message = message
    }

    override fun deserialize(deserializer: org.ndk.nexushub.packet.serialize.PacketDeserializer) {
        message = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(message)
    }

    override fun toString(): String {
        return "PacketOk(message='$message')"
    }

}