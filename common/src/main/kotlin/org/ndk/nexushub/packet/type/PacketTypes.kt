@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.packet.type

import org.ndk.nexushub.packet.*

object PacketTypes {
    val entries = ArrayList<PacketType<*>>(20)

    val OK = new<PacketOk>()
    val ERROR = new<PacketError>()
    val AUTH = new<PacketAuth>()
    val LOAD_DATA = new<PacketLoadData>()
    val SAVE_DATA = new<PacketSaveData>()
    val BATCH_SAVE_DATA = new<PacketBatchSaveData>()
    val CREATE_SESSION = new<PacketCreateSession>()
    val STOP_SESSION = new<PacketStopSession>()
    val USER_DATA = new<PacketUserData>()
    val REQUEST_SYNC = new<PacketRequestSync>()
    val REQUEST_HOLDER_SYNC = new<PacketRequestHolderSync>()
    val REQUEST_LEADERBOARD = new<PacketRequestLeaderboard>()
    val LEADERBOARD = new<PacketLeaderboard>()
    val REQUEST_TOP_POSITION = new<PacketRequestTopPosition>()
    val TOP_POSITION = new<PacketTopPosition>()


    internal inline fun <reified T : Packet> new(): PacketType<T> {
        val clazz = T::class.java
        check(clazz.constructors.any { it.parameterCount == 0 }) {
            "Packet '${clazz.name}' class should have empty constructor"
        }
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
