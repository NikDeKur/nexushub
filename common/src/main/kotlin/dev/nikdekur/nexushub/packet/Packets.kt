/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet

import dev.nikdekur.ndkore.ext.buildRepresentation
import dev.nikdekur.nexushub.packet.serialize.PacketDeserializer
import dev.nikdekur.nexushub.packet.serialize.PacketSerializer


/**
 * @property packetId Packet id that should be unique for each packet
 * to websocket's determining packet class
 */
abstract class Packet {

    abstract val packetId: UByte

    private var _sequantial: UShort? = null
    var sequantial: UShort
        get() = _sequantial ?: error("Sequantial is not set")
        set(value) { _sequantial = value }

    val responseSequential
        get() = sequantial.inc()

    abstract fun deserialize(deserializer: PacketDeserializer)
    abstract fun serialize(serializer: PacketSerializer)

    fun serialize(): ByteArray {
        val serializer = PacketSerializer(this)
        serialize(serializer)
        return serializer.finish()
    }

    override fun toString(): String {
        return buildRepresentation(this)
    }

    abstract class Scope : Packet() {
        abstract val scopeId: String
    }

    abstract class Session : Scope() {
        abstract val holderId: String
    }
}