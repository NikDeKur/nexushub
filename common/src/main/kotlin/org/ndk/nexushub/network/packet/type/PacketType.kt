package org.ndk.nexushub.network.packet.type

import org.ndk.klib.construct
import org.ndk.nexushub.network.packet.Packet

interface PacketType<T : Packet> {

    val id: UByte
    val clazz: Class<out T>

    fun newInstance(): T {
        return clazz.construct()
    }
}