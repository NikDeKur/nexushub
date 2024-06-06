@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.network.packet.serialize

import java.nio.ByteBuffer

class PacketDeserializer(data: ByteArray) {

    val buffer: ByteBuffer = ByteBuffer.wrap(data)

    fun readBoolean(): Boolean {
        return readByte() == 1.toByte()
    }

    fun readByte(): Byte {
        return buffer.get()
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

    fun <T> readList(size: Int = readInt(), reader: PacketDeserializer.(Int) -> T): List<T> {
        val list = List(size) { this.reader(it) }
        return list
    }

    fun <K, V> readMap(size: Int = readInt(), keyReader: PacketDeserializer.(Int) -> K, valueReader: PacketDeserializer.(Int) -> V): Map<K, V> {
        val map = HashMap<K, V>(size)
        repeat(size) {
            map[this.keyReader(it)] = this.valueReader(it)
        }
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
        buffer.position(BYTES_MARGIN)
    }

    fun finish() {
        buffer.clear()
    }

    companion object {
        const val BYTES_MARGIN = 2
    }
}