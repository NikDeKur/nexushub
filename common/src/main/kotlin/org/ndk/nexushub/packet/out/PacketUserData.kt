package org.ndk.nexushub.packet.out

import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes.USER_DATA


/**
 * (OUT) Packet to send requested user data from the database
 */
class PacketUserData : Packet {

    override val packetId = USER_DATA.id

    var holderId: String = ""
    var scopeId: String = ""
    var data: String = ""

    constructor()
    constructor(userId: String, scopeId: String, data: String) {
        this.holderId = userId
        this.scopeId = scopeId
        this.data = data
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        holderId = deserializer.readString()
        scopeId = deserializer.readString()
        data = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(holderId)
        serializer.writeString(scopeId)
        serializer.writeString(data)
    }

    override fun toString(): String {
        return "PacketUserData(userId='$holderId', scopeId='$scopeId', data='$data')"
    }
}