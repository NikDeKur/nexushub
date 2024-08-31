/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.packet.type

import dev.nikdekur.nexushub.packet.*
import dev.nikdekur.nexushub.packet.PacketAuth
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

object PacketTypes {
    var entriesSize = 0
    val entries = Array<PacketType?>(UByte.MAX_VALUE.toInt()) { null }

    val OK = new<PacketOk>()
    val ERROR = new<PacketError>()
    val AUTH = new<PacketAuth>()
    val HELLO = new<PacketHello>()
    val HEARTBEAT = new<PacketHeartbeat>()
    val HEARTBEAT_ACK = new<PacketHeartbeatACK>()
    val READY = new<PacketReady>()
    val LOAD_DATA = new<PacketLoadData>()
    val SAVE_DATA = new<PacketSaveData>()
    val BATCH_SAVE_DATA = new<PacketBatchSaveData>()
    val STOP_SESSION = new<PacketStopSession>()
    val END_SESSION = new<PacketEndSession>()
    val USER_DATA = new<PacketUserData>()
    val REQUEST_SYNC = new<PacketRequestSync>()
    val REQUEST_LEADERBOARD = new<PacketRequestLeaderboard>()
    val LEADERBOARD = new<PacketLeaderboard>()
    val REQUEST_TOP_POSITION = new<PacketRequestTopPosition>()
    val TOP_POSITION = new<PacketTopPosition>()


    internal inline fun <reified T : Packet> new(): PacketType {
        return object : PacketType {

            override val id = entriesSize.toUByte()

            @OptIn(InternalSerializationApi::class)
            override val serializer: KSerializer<Packet> = T::class.serializer() as KSerializer<Packet>
        }.also {
            entries[entriesSize] = it
            entriesSize++
        }
    }


    fun fromId(id: UByte): PacketType? {
        return entries.getOrNull(id.toInt())
    }
}
