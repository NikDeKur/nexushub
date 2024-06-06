package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes

class PacketBatchSaveData : Packet() {
    override val packetId = PacketTypes.BATCH_SAVE_DATA.id

    var scopeId: String = ""

    /**
     * HolderId to JSON Data
     */
    var data: Map<String, String> = emptyMap()

    override fun deserialize(deserializer: PacketDeserializer) {
        scopeId = deserializer.readString()
        data = deserializer.readMap(
            keyReader = { readString() },
            valueReader = { readString() }
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