package org.ndk.nexushub.packet.type

import org.ndk.klib.construct
import org.ndk.nexushub.packet.Packet

interface PacketType<T : org.ndk.nexushub.packet.Packet> {

    val id: UByte
    val clazz: Class<out T>

    fun newInstance(): T {
        return clazz.construct()
    }
}