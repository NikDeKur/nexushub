package org.ndk.nexushub.packet.type

import dev.nikdekur.ndkore.ext.construct
import org.ndk.nexushub.packet.Packet


interface PacketType<T : Packet> {

    val id: UByte
    val clazz: Class<out T>

    fun newInstance(): T {
        return clazz.construct()
    }
}