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
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.packet.`in`.PacketBatchSaveData
import dev.nikdekur.nexushub.packet.`in`.PacketEndSession
import dev.nikdekur.nexushub.packet.`in`.PacketHeartbeat
import dev.nikdekur.nexushub.packet.`in`.PacketHello
import dev.nikdekur.nexushub.packet.`in`.PacketLoadData
import dev.nikdekur.nexushub.packet.`in`.PacketRequestLeaderboard
import dev.nikdekur.nexushub.packet.`in`.PacketRequestTopPosition
import dev.nikdekur.nexushub.packet.`in`.PacketSaveData
import dev.nikdekur.nexushub.packet.out.PacketHeartbeatACK
import dev.nikdekur.nexushub.packet.out.PacketLeaderboard
import dev.nikdekur.nexushub.packet.out.PacketReady
import dev.nikdekur.nexushub.packet.out.PacketRequestSync
import dev.nikdekur.nexushub.packet.out.PacketStopSession
import dev.nikdekur.nexushub.packet.out.PacketTopPosition
import dev.nikdekur.nexushub.packet.out.PacketUserData

object PacketTypes {
    var entriesSize = 0
    val entries = Array<PacketType<*>?>(UByte.MAX_VALUE.toInt()) { null }

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


    internal inline fun <reified T : Packet> new(): PacketType<T> {
        val clazz = T::class.java
        require(clazz.constructors.any { it.parameterCount == 0 }) {
            "Packet '${clazz.name}' class should have an empty constructor!"
        }

        return object : PacketType<T> {
            override val id = entriesSize.toUByte()
            override val clazz = clazz
        }.also {
            entries[entriesSize] = it
            entriesSize++
        }
    }


    fun fromId(id: UByte): PacketType<*>? {
        return entries.getOrNull(id.toInt())
    }

    inline fun newInstanceFromId(id: UByte): Packet? {
        return fromId(id)?.newInstance()
    }
}
