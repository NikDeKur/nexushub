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
import dev.nikdekur.nexushub.packet.type.PacketTypes
import kotlinx.serialization.Serializable

@Serializable
data class PacketError(
    var level: Level,
    var code: Code,
    val message: String
) : Packet() {

    override fun getType() = PacketTypes.ERROR

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


@Serializable
data class PacketOk(
    val message: String
) : Packet() {
    override fun getType() = PacketTypes.OK
}