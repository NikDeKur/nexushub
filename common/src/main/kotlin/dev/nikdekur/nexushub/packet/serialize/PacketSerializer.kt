/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.packet.serialize

import dev.nikdekur.nexushub.packet.Packet
import java.math.BigInteger
import java.nio.ByteBuffer

class PacketSerializer(val packet: Packet) {

    private var byteBuffer: ByteBuffer = ByteBuffer.allocate(16)

    init {
        writeByte(packet.packetId.toByte())
        writeShort(packet.sequantial.toShort())
    }

    private fun ensureCapacity(bytes: Int) {
        if (byteBuffer.remaining() < bytes) {
            val newBuffer = ByteBuffer.allocate(byteBuffer.capacity() * 2 + bytes)
            byteBuffer.flip()
            newBuffer.put(byteBuffer)
            byteBuffer = newBuffer
        }
    }

    fun writeByteArray(value: ByteArray) {
        ensureCapacity(4 + value.size)
        writeInt(value.size)
        byteBuffer.put(value)
    }


    fun writeByte(value: Byte) {
        ensureCapacity(1)
        byteBuffer.put(value)
    }

    fun writeShort(value: Short) {
        ensureCapacity(2)
        byteBuffer.putShort(value)
    }

    fun writeInt(value: Int) {
        ensureCapacity(4)
        byteBuffer.putInt(value)
    }

    fun writeLong(value: Long) {
        ensureCapacity(8)
        byteBuffer.putLong(value)
    }

    fun writeFloat(value: Float) {
        ensureCapacity(4)
        byteBuffer.putFloat(value)
    }

    fun writeDouble(value: Double) {
        ensureCapacity(8)
        byteBuffer.putDouble(value)
    }

    fun writeBigInteger(value: BigInteger) {
        writeByteArray(value.toByteArray())
    }


    fun writeNumber(value: Number) {
        when (value) {
            is Byte -> writeByte(value)
            is Short -> writeShort(value)
            is Int -> writeInt(value)
            is Long -> writeLong(value)
            is Float -> writeFloat(value)
            is Double -> writeDouble(value)
            is BigInteger -> writeBigInteger(value)
            else -> throw IllegalArgumentException("Unsupported number type: ${value::class.simpleName}")
        }
    }


    fun writeChar(value: Char) {
        ensureCapacity(2)
        byteBuffer.putChar(value)
    }

    fun writeString(value: String) {
        ensureCapacity(4 + value.length)
        val bytes = value.toByteArray()
        writeInt(bytes.size)
        byteBuffer.put(bytes)
    }

    fun writeBoolean(value: Boolean) {
        ensureCapacity(1)
        byteBuffer.put(if (value) 1 else 0)
    }

    fun <T> writeList(list: List<T>, size: Number = list.size, writer: PacketSerializer.(T) -> Unit) {
        writeNumber(size)
        list.forEach {
            this.writer(it)
        }
    }

    fun <K, V> writeMap(
        map: Map<K, V>,
        size: Number = map.size,
        keyWriter: PacketSerializer.(K) -> Unit,
        valueWriter: PacketSerializer.(V) -> Unit
    ) {
        writeNumber(size)
        map.forEach { (k, v) ->
            this.keyWriter(k)
            this.valueWriter(v)
        }
    }

    fun finish(): ByteArray {
        byteBuffer.flip()
        val bytes = ByteArray(byteBuffer.limit())
        byteBuffer[bytes]
        return bytes
    }


}