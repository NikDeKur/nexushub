/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet

import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.packet.type.PacketTypes
import kotlinx.serialization.Serializable

@Serializable
class PacketHeartbeatACK : Packet() {
    override fun getType() = PacketTypes.HEARTBEAT_ACK
}


@Serializable
class PacketHello : Packet() {
    override fun getType() = PacketTypes.HELLO
}


@Serializable
data class PacketLeaderboard(
    val startFrom: Int,
    val leaderboard: Leaderboard,
    val requestPosition: LeaderboardEntry?
) : Packet() {
    override fun getType() = PacketTypes.LEADERBOARD
}


@Serializable
data class PacketReady(
    val heartbeatInterval: Long
) : Packet() {
    override fun getType() = PacketTypes.READY
}

@Serializable
data class PacketRequestSync(
    override val scopeId: String
) : Packet.Scope() {
    override fun getType() = PacketTypes.REQUEST_SYNC
}

@Serializable
data class PacketStopSession(
    override val scopeId: String,
    override val holderId: String
) : Packet.Session() {
    override fun getType() = PacketTypes.STOP_SESSION
}

@Serializable
data class PacketTopPosition(
    val entry: LeaderboardEntry?
) : Packet() {
    override fun getType() = PacketTypes.TOP_POSITION
}

@Serializable
data class PacketUserData(
    override val scopeId: String,
    override val holderId: String,
    val data: String
) : Packet.Session() {
    override fun getType() = PacketTypes.USER_DATA
}