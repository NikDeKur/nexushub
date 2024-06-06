@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.network.packet.type

import org.ndk.nexushub.network.packet.*
import java.util.concurrent.CopyOnWriteArrayList

object PacketTypes {
    val entries = CopyOnWriteArrayList<PacketType<*>>()

    val OK = new(PacketOk::class.java)
    val ERROR = new(PacketError::class.java)
    val PING = new(PacketPing::class.java)
    val PONG = new(PacketPong::class.java)
    val AUTH = new(PacketAuth::class.java)
    val LOAD_DATA = new(PacketLoadData::class.java)
    val SAVE_DATA = new(PacketSaveData::class.java)
    val BATCH_SAVE_DATA = new(PacketBatchSaveData::class.java)
    val CREATE_SESSION = new(PacketCreateSession::class.java)
    val STOP_SESSION = new(PacketStopSession::class.java)
    val USER_DATA = new(PacketUserData::class.java)
    val REQUEST_SYNC = new(PacketRequestSync::class.java)
    val REQUEST_HOLDER_SYNC = new(PacketRequestHolderSync::class.java)
    val REQUEST_LEADERBOARD = new(PacketRequestLeaderboard::class.java)
    val LEADERBOARD = new(PacketLeaderboard::class.java)
    val REQUEST_TOP_POSITION = new(PacketRequestTopPosition::class.java)
    val TOP_POSITION = new(PacketTopPosition::class.java)


    internal fun <T : Packet> new(clazz: Class<out T>): PacketType<T> {
        return object : PacketType<T> {
            override val id = entries.size.toUByte()
            override val clazz = clazz
        }.also(entries::add)
    }


    fun fromId(id: UByte): PacketType<*>? {
        return entries.getOrNull(id.toInt())
    }

    inline fun newInstanceFromId(id: UByte): Packet? {
        return fromId(id)?.newInstance()
    }
}
