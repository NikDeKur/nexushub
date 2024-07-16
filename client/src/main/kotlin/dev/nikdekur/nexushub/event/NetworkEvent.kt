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
import kotlin.getValue

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

    class Sync(context: IncomingContext<PacketRequestSync>) : NetworkEvent(context) {
        val packet by context::packet
        val scopeId by packet::scopeId
    }


    class StopSession(context: IncomingContext<PacketStopSession>) : NetworkEvent(context) {
        val packet by context::packet
        val holderId by packet::holderId
        val scopeId by packet::scopeId
    }

    class TopPosition(context: IncomingContext<PacketTopPosition>) : NetworkEvent(context) {
        val packet by context::packet
        val entry by packet::entry
    }


    class UserData(context: IncomingContext<PacketUserData>) : NetworkEvent(context) {
        val packet by context::packet
        val holderId by packet::holderId
        val scopeId by packet::scopeId
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

