package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer

class PacketSaveError : PacketError {

    var errors: Map<String, String> = emptyMap()

    constructor() : super()
    constructor(message: String, errors: Map<String, String>) : super(Level.ERROR, message) {
        this.errors = errors
    }

    override fun serialize(serializer: PacketSerializer) {
        super.serialize(serializer)
        serializer.writeMap(errors,
            keyWriter = { serializer.writeString(it) },
            valueWriter = { serializer.writeString(it) }
        )
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        super.deserialize(deserializer)
        errors = deserializer.readMap(
            keyReader = { deserializer.readString() },
            valueReader = { deserializer.readString() }
        )
    }
}