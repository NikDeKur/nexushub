package org.ndk.nexushub.packet.`in`

import org.ndk.nexushub.packet.Packet
import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes

class PacketBatchSaveData : Packet {
    override val packetId = PacketTypes.BATCH_SAVE_DATA.id

    var scopeId: String = ""

    /**
     * HolderId to JSON Data
     */
    var data: Map<String, String> = emptyMap()

    constructor()
    constructor(scopeId: String, data: Map<String, String>) {
        this.scopeId = scopeId
        this.data = data
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        val size = deserializer.readInt()
        data = deserializer.readHashMap(
            size = size,
            keyReader = { readString() },
            valueReader = { readString() },
        )
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeString(scopeId)
        serializer.writeMap(data,
            keyWriter = { writeString(it) },
            valueWriter = { writeString(it) }
        )
    }

    override fun toString(): String {
        return "PacketBatchSaveData(scopeId='$scopeId', data=$data)"
    }
}