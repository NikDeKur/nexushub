package org.ndk.nexushub.packet

import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.serialize.PacketSerializer


/**
 * @property packetId Packet id that should be unique for each packet
 * to websocket's determining packet class
 */
abstract class Packet {

    abstract val packetId: UByte

    private var _sequantial: UShort? = null
    var sequantial: UShort
        get() = _sequantial ?: error("Sequantial is not set")
        set(value) { _sequantial = value }

    val responseSequential
        get() = sequantial.inc()

    abstract fun deserialize(deserializer: PacketDeserializer)
    abstract fun serialize(serializer: PacketSerializer)

    fun serialize(): ByteArray {
        val serializer = PacketSerializer(this)
        serialize(serializer)
        return serializer.finish()
    }

    abstract override fun toString(): String
}