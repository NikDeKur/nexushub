package org.ndk.nexushub.packet

import org.ndk.klib.buildRepresentation
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes

class PacketRequestLeaderboard : Packet {

    override val packetId = PacketTypes.REQUEST_LEADERBOARD.id

    var scopeId: String = ""
    var field: String = ""
    var startFrom: Int = 0
    var limit: Int = 0

    var requestPosition: String = ""

    constructor() : super()
    constructor(scopeId: String, filter: String, startFrom: Int, limit: Int, requestPosition: String) : super() {
        this.scopeId = scopeId
        this.field = filter
        this.startFrom = startFrom
        this.limit = limit
        this.requestPosition = requestPosition
    }

    override fun deserialize(deserializer: org.ndk.nexushub.packet.serialize.PacketDeserializer) {
        scopeId = deserializer.readString()
        field = deserializer.readString()
        startFrom = deserializer.readInt()
        limit = deserializer.readInt()
        requestPosition = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeString(field)
        serializer.writeInt(startFrom)
        serializer.writeInt(limit)
        serializer.writeString(requestPosition)
    }

    override fun toString(): String {
        return buildRepresentation(this)
    }
}