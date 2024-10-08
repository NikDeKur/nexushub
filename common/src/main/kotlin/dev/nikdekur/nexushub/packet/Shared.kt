/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet

import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.packet.type.PacketTypes
import kotlinx.serialization.Serializable

enum class ErrorLevel {
    WARNING,
    ERROR,
    FATAL
}

enum class ErrorCode(val defLevel: ErrorLevel, val defMessage: String) {
    UNKNOWN(ErrorLevel.ERROR, "Unknown error"),
    SCOPE_IS_NOT_ALLOWED(ErrorLevel.ERROR, "Scope is not allowed"),
    SESSION_ALREADY_EXISTS(ErrorLevel.ERROR, "Session already exists"),
    SESSION_NOT_FOUND(ErrorLevel.ERROR, "Session not found"),
    ERROR_IN_DATA(ErrorLevel.ERROR, "Data is not valid "),
    FIELD_IS_NOT_NUMBER(ErrorLevel.ERROR, "Field is not a number, that can be converted to double!"),
}

@Serializable
data class PacketError(
    val level: ErrorLevel,
    val code: ErrorCode,
    val message: String
) : Packet() {

    override fun getType() = PacketTypes.ERROR
}

suspend inline fun IncomingContext<out Packet>.respondError(code: ErrorCode) =
    respond<Unit>(PacketError(code.defLevel, code, code.defMessage))


@Serializable
data class PacketOk(
    val message: String
) : Packet() {
    override fun getType() = PacketTypes.OK
}