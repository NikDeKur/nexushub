package org.ndk.nexushub.network.packet

import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.serialize.PacketSerializer
import org.ndk.nexushub.network.packet.type.PacketTypes.ERROR

open class PacketError : Packet {

    override val packetId = ERROR.id

    var level: Level = Level.WARNING
    var message: String = ""


    constructor()
    constructor(level: Level, message: String) {
        this.level = level
        this.message = message
    }

    override fun deserialize(deserializer: PacketDeserializer) {
        level = Level.entries[deserializer.readByte().toInt()]
        message = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeByte(level.ordinal.toByte())
        serializer.writeString(message)
    }

    override fun toString(): String {
        return "PacketError(level=$level, message='$message')"
    }


    enum class Level {
        WARNING,
        ERROR,
        FATAL
    }
}