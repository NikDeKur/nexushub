/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

import dev.nikdekur.ndkore.ext.randInt
import dev.nikdekur.ndkore.map.MutableSetsMap
import dev.nikdekur.ndkore.map.add
import dev.nikdekur.nexushub.network.dsl.HandlerContext
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.network.timeout.TimeoutService
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.network.transmission.RuntimePacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.type.PacketTypes
import dev.nikdekur.serialization.barray.ByteArrayFormat
import kotlinx.coroutines.suspendCancellableCoroutine
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Predicate
import kotlin.coroutines.resume

open class RuntimePacketController(
    val talker: Talker,
    val timeoutService: TimeoutService
) : PacketController {

    val logger = LoggerFactory.getLogger(javaClass)

    val responses = ConcurrentHashMap<UShort, PacketTransmission<*>>()
    val waiters: MutableSetsMap<Class<out Packet>, (IncomingContext<Packet>) -> Unit> = ConcurrentHashMap()

    val random = SecureRandom()

    val barray = ByteArrayFormat()

    fun newSequential(): UShort {
        var seq: UShort
        do {
            // Random ushort
            seq = random.randInt(0, 65535 + 1).toUShort()
        } while (responses.containsKey(seq))
        return seq
    }

    override suspend fun <R> newTransmission(
        packet: Packet,
        reaction: PacketReaction<R>,
        respondTo: UShort?
    ): PacketTransmission<R> {
        return RuntimePacketTransmission(talker, packet, reaction, respondTo)
    }

    override suspend fun processSending(transmission: PacketTransmission<*>): ByteArray {
        val reaction = transmission.reaction
        val sequential = transmission.respondTo?.inc() ?: newSequential()
        val nextSequential = sequential.inc()
        val packet = transmission.packet.let {
            it.sequantial = sequential
            val array = barray.encodeToByteArray(it.getType().serializer, it)

            // 1 byte for packet type
            // 2 bytes for sequential (ushort)
            val packet = ByteArray(array.size + 1 + 2)

            // Write a packet type and sequential
            ByteBuffer.wrap(packet)
                .put(it.getType().id.toByte())
                .putShort(sequential.toShort())

            array.copyInto(packet, 3)

            packet
        }

        responses[nextSequential] = transmission

        reaction.timeouts.forEach {
            val timeout = it.key
            timeoutService.scheduleTimeout(talker, timeout) {

                // Check that we haven't yet received a packet
                // If the packet came just in time of timeout we don't want to invoke timeout
                if (transmission.received) return@scheduleTimeout

                // Check if the talker is still open
                if (!talker.isOpen) return@scheduleTimeout

                transmission.onTimeout(timeout)
                responses.remove(nextSequential)
            }
        }

        return packet
    }


    override suspend fun processReceiving(bytes: ByteArray): IncomingContext<Packet>? {
        var seq: UShort? = null
        var response: PacketTransmission<*>? = null
        try {
            val byteBuffer = ByteBuffer.wrap(bytes)
            val packetId = byteBuffer.get().toUByte()
            seq = byteBuffer.short.toUShort()
            val type = PacketTypes.fromId(packetId) ?: return null

            val data = bytes.copyOfRange(3, bytes.size)
            val instance = barray.decodeFromByteArray(type.serializer, data)
            instance.sequantial = seq

            response = responses.remove(seq)
            response?.onReceive(talker, instance)

            val context = HandlerContext.Receive<Packet, Unit>(talker, instance, response != null)
            waiters[instance::class.java]
                ?.forEach { it(context) }

            return context
        } catch (e: Exception) {
            response?.onException(talker, e)
            e.printStackTrace()
            return null
        } finally {
            seq?.let(responses::remove)
        }
    }

    override suspend fun <T : Packet> wait(packetClass: Class<T>, condition: Predicate<T>): IncomingContext<T> {
        return suspendCancellableCoroutine { continuation ->
            waiters.add(packetClass, { context ->
                @Suppress("UNCHECKED_CAST")
                val context = context as IncomingContext<T>
                val packet = context.packet
                if (!condition.test(packet)) return@add
                waiters.remove(packetClass)
                continuation.resume(context)
            })

            continuation.invokeOnCancellation {
                waiters.remove(packetClass)
            }
        }
    }
}