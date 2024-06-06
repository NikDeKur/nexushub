package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes

class PacketRequestLeaderboard : Packet {

    override val packetId = PacketTypes.REQUEST_LEADERBOARD.id

    var scopeId: String = ""
    var holderId: String = ""
    var field: String = ""
    var limit: Int = 0

    constructor() : super()
    constructor(scopeId: String, holderId: String, filter: String, limit: Int) : super() {
        this.scopeId = scopeId
        this.holderId = holderId
        this.field = filter
        this.limit = limit
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        holderId = deserializer.readString()
        field = deserializer.readString()
        limit = deserializer.readInt()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeString(holderId)
        serializer.writeString(field)
        serializer.writeInt(limit)
    }

    override fun toString(): String {
        return "PacketRequestLeaderboard(scopeId='$scopeId', holderId='$holderId', field='$field', limit=$limit)"
    }
}