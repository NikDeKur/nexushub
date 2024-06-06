package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes

class PacketTopPosition : Packet {
    override val packetId = PacketTypes.TOP_POSITION.id

    var position: Long = 0
    var value: Double = 0.0

    constructor() : super()
    constructor(position: Long, value: Double) : super() {
        this.position = position
        this.value = value
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        position = deserializer.readLong()
        value = deserializer.readDouble()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeLong(position)
        serializer.writeDouble(value)
    }

    override fun toString(): String {
        return "PacketTopPosition(position=$position, value=$value)"
    }


}