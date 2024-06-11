package org.ndk.nexushub.packet

import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes.SAVE_DATA


/**
 * (IN) Packet to save user data from the database
 *
 * Requires authentication to be processed
 */
open class PacketSaveData : Packet {

    override val packetId = SAVE_DATA.id

    var holderId: String = ""
    var scopeId: String = ""
    var data: String = "" // JSON

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
        return "PacketSaveData(holderId='$holderId', scopeId='$scopeId', data='$data')"
    }
}