package org.ndk.nexushub.packet

import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.packet.PacketError.Code
import org.ndk.nexushub.packet.serialize.PacketSerializer
import org.ndk.nexushub.packet.type.PacketTypes


open class PacketError : Packet {

    override val packetId = PacketTypes.ERROR.id

    var level: Level = Level.WARNING
    var code: Code = Code.UNKNOWN
    var message: String = ""


    constructor()
    constructor(level: Level, code: Code, message: String) {
        this.level = level
        this.code = code
        this.message = message
    }

    override fun deserialize(deserializer: org.ndk.nexushub.packet.serialize.PacketDeserializer) {
        level = Level.entries[deserializer.readByte().toInt()]
        level = Level.entries[deserializer.readByte().toInt()]
        message = deserializer.readString()
    }

    override fun serialize(serializer: PacketSerializer) {
        serializer.writeByte(level.ordinal.toByte())
        serializer.writeByte(code.ordinal.toByte())
        serializer.writeString(message)
    }

    override fun toString(): String {
        return "PacketError(level=$level, code=$code, message='$message')"
    }


    enum class Level {
        WARNING,
        ERROR,
        FATAL
    }

    enum class Code(val defLevel: Level, val defMessage: String) {
        UNKNOWN(Level.ERROR, "Unknown error"),
        SCOPE_IS_NOT_ALLOWED(Level.ERROR, "Scope is not allowed"),
        SESSION_ALREADY_EXISTS(Level.ERROR, "Session already exists"),
        SESSION_NOT_FOUND(Level.ERROR, "Session not found"),
        ERROR_IN_DATA(Level.ERROR, "Data is not valid "),
        FIELD_IS_NOT_NUMBER(Level.ERROR, "Field is not a number, that can be converted to double!"),
    }
}

suspend inline fun IncomingContext<out Packet>.respondError(code: Code) =
    respond(PacketError(code.defLevel, code, code.defMessage))