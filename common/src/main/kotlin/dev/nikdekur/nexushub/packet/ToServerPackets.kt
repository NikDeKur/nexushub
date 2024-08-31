/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet

import dev.nikdekur.nexushub.packet.type.PacketTypes
import kotlinx.serialization.Serializable

/**
 * (IN) Packet for authentication
 *
 * All clients must send this packet first to authenticate.
 */
@Serializable
data class PacketAuth(
    val login: String,
    val password: String,
    val node: String
) : Packet() {
    override fun getType() = PacketTypes.AUTH
}

@Serializable
class PacketHeartbeat : Packet() {
    override fun getType() = PacketTypes.HEARTBEAT
}


@Serializable
data class PacketSaveData(
    override val scopeId: String,
    override val holderId: String,
    val data: String
) : Packet.Session() {
    override fun getType() = PacketTypes.SAVE_DATA
}

@Serializable
data class PacketBatchSaveData(
    val scopeId: String,
    val data: Map<String, String>
) : Packet() {
    override fun getType() = PacketTypes.BATCH_SAVE_DATA
}

@Serializable
data class PacketEndSession(
    override val scopeId: String,
    override val holderId: String,
    val data: String
) : Packet.Session() {
    override fun getType() = PacketTypes.END_SESSION
}


@Serializable
data class PacketLoadData(
    override val scopeId: String,
    override val holderId: String
) : Packet.Session() {
    override fun getType() = PacketTypes.LOAD_DATA
}

@Serializable
data class PacketRequestLeaderboard(
    override val scopeId: String,
    val field: String,
    val startFrom: Int,
    val limit: Int,
    val requestPosition: String
) : Packet.Scope() {
    override fun getType() = PacketTypes.REQUEST_LEADERBOARD
}

@Serializable
data class PacketRequestTopPosition(
    override val scopeId: String,
    override val holderId: String,
    val field: String
) : Packet.Session() {
    override fun getType() = PacketTypes.REQUEST_TOP_POSITION
}