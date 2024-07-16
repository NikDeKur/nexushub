/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet

import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.packet.PacketError.Code
import dev.nikdekur.nexushub.packet.serialize.PacketSerializer
import dev.nikdekur.nexushub.packet.type.PacketTypes


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

    override fun deserialize(deserializer: dev.nikdekur.nexushub.packet.serialize.PacketDeserializer) {
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
    respond<Unit>(PacketError(code.defLevel, code, code.defMessage))