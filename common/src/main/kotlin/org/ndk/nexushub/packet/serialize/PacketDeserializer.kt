@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.packet.serialize

import java.nio.ByteBuffer

class PacketDeserializer(data: ByteArray) {

    val buffer: ByteBuffer = ByteBuffer.wrap(data)

    fun readByte(): Byte {
        return buffer.get()
    }

    fun readBoolean(): Boolean {
        return readByte() == org.ndk.nexushub.packet.serialize.PacketDeserializer.Companion.BYTE1
    }

    inline fun readUByte() = readByte().toUByte()

    fun readShort(): Short {
        return buffer.short
    }

    fun readInt(): Int {
        return buffer.int
    }

    fun readLong(): Long {
        return buffer.long
    }

    fun readFloat(): Float {
        return buffer.float
    }

    fun readDouble(): Double {
        return buffer.double
    }

    fun readString(): String {
        val length = buffer.getInt()
        val bytes = ByteArray(length)
        buffer[bytes]
        return String(bytes)
    }

    fun readList(size: Int = readInt(), reader: org.ndk.nexushub.packet.serialize.PacketDeserializer.(Int) -> Unit) {
        repeat(size) {
            this.reader(it)
        }
    }

    inline fun <K : Any, V> readMap(
        size: Int = readInt(),
        keyReader: org.ndk.nexushub.packet.serialize.PacketDeserializer.(Int) -> K,
        valueReader: org.ndk.nexushub.packet.serialize.PacketDeserializer.(Int) -> V,
        executor: (K, V) -> Unit
    ) {
        repeat(size) {
            executor(this.keyReader(it), this.valueReader(it))
        }
    }

    inline fun <K : Any, V> readHashMap(
        size: Int = readInt(),
        keyReader: org.ndk.nexushub.packet.serialize.PacketDeserializer.(Int) -> K,
        valueReader: org.ndk.nexushub.packet.serialize.PacketDeserializer.(Int) -> V
    ): HashMap<K, V> {
        val map = HashMap<K, V>()
        readMap(size, keyReader, valueReader, map::put)
        return map
    }

    /**
     * Rewind buffer to start (0), to read packet data again
     */
    fun reset() {
        buffer.rewind()
    }

    /**
     * Set buffer position to [BYTES_MARGIN] to skip packet id
     */
    fun focusData() {
        buffer.position(org.ndk.nexushub.packet.serialize.PacketDeserializer.Companion.BYTES_MARGIN)
    }

    fun finish() {
        buffer.clear()
    }

    companion object {
        const val BYTES_MARGIN = 2
        val BYTE1 = 1.toByte()
    }
}