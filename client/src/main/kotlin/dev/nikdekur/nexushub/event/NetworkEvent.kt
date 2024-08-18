/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.event

import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.`in`.PacketHello
import dev.nikdekur.nexushub.packet.out.PacketHeartbeatACK
import dev.nikdekur.nexushub.packet.out.PacketLeaderboard
import dev.nikdekur.nexushub.packet.out.PacketReady
import dev.nikdekur.nexushub.packet.out.PacketRequestSync
import dev.nikdekur.nexushub.packet.out.PacketStopSession
import dev.nikdekur.nexushub.packet.out.PacketTopPosition
import dev.nikdekur.nexushub.packet.out.PacketUserData

sealed class NetworkEvent(val context: IncomingContext<out Packet>) : Event() {

    suspend inline fun <R> respond(
        packet: Packet,
        noinline builder: PacketReaction.Builder<R>.() -> Unit = {}
    ) = context.respond(packet, builder)

    @JvmName("respondUnit")
    suspend inline fun respond(
        packet: Packet,
        noinline builder: PacketReaction.Builder<Unit>.() -> Unit = {}
    ) = respond<Unit>(packet, builder)


    sealed class ScopeEvent(context: IncomingContext<out Packet.Scope>) : NetworkEvent(context) {
        val scopeId by context.packet::scopeId
    }

    sealed class SessionEvent(context: IncomingContext<out Packet.Session>) : ScopeEvent(context) {
        val holderId by context.packet::holderId
    }


    class Hello(context: IncomingContext<PacketHello>) : NetworkEvent(context)

    class ReadyEvent(context: IncomingContext<PacketReady>) : NetworkEvent(context) {
        val packet by context::packet
        val heartbeatInterval by packet::heartbeatInterval
    }

    class HeartbeatACK(context: IncomingContext<PacketHeartbeatACK>) : NetworkEvent(context)

    class Leaderboard(context: IncomingContext<PacketLeaderboard>) : NetworkEvent(context) {
        val packet by context::packet
        val leaderboard by packet::leaderboard
        val position by packet::requestPosition
    }

    class Sync(context: IncomingContext<PacketRequestSync>) : ScopeEvent(context)


    class StopSession(context: IncomingContext<PacketStopSession>) : SessionEvent(context) {
        val packet by context::packet
    }

    class TopPosition(context: IncomingContext<PacketTopPosition>) : NetworkEvent(context) {
        val packet by context::packet
        val entry by packet::entry
    }


    class UserData(context: IncomingContext<PacketUserData>) : SessionEvent(context) {
        val packet by context::packet
        val data by packet::data
    }


    companion object {

        fun decode(context: IncomingContext<*>): NetworkEvent {
            val packet = context.packet
            @Suppress("UNCHECKED_CAST")
            return when (packet) {
                is PacketHello -> Hello(context as IncomingContext<PacketHello>)
                is PacketReady -> ReadyEvent(context as IncomingContext<PacketReady>)
                is PacketHeartbeatACK -> HeartbeatACK(context as IncomingContext<PacketHeartbeatACK>)
                is PacketLeaderboard -> Leaderboard(context as IncomingContext<PacketLeaderboard>)
                is PacketRequestSync -> Sync(context as IncomingContext<PacketRequestSync>)
                is PacketStopSession -> StopSession(context as IncomingContext<PacketStopSession>)
                is PacketTopPosition -> TopPosition(context as IncomingContext<PacketTopPosition>)
                is PacketUserData -> UserData(context as IncomingContext<PacketUserData>)
                else -> throw IllegalArgumentException("Unknown packet type: $packet")
            }
        }
    }
}

