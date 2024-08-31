/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet

import dev.nikdekur.nexushub.packet.type.PacketType
import kotlinx.serialization.Serializable


/**
 * @property packetId Packet id that should be unique for each packet
 * to websocket's determining packet class
 */
@Serializable
abstract class Packet {

    abstract fun getType(): PacketType

    private var _sequantial: UShort? = null
    var sequantial: UShort
        get() = _sequantial ?: error("Sequantial is not set")
        set(value) {
            _sequantial = value
        }

    abstract class Scope : Packet() {
        abstract val scopeId: String
    }

    abstract class Session : Scope() {
        abstract val holderId: String
    }
}