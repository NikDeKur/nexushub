/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

import dev.nikdekur.ndkore.ext.randInt
import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.nexushub.network.dsl.HandlerContext
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.serialize.PacketDeserializer
import dev.nikdekur.nexushub.packet.type.PacketTypes.newInstanceFromId
import kotlinx.coroutines.CoroutineDispatcher
import org.slf4j.LoggerFactory
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

open class PacketManager(val talker: Talker, timeoutsDispatcher: CoroutineDispatcher) {

    val logger = LoggerFactory.getLogger("PacketManager")

    // Create a scheduler for timeouts
    val scheduler = CoroutineScheduler.fromSupervisor(timeoutsDispatcher)

    val responses = ConcurrentHashMap<UShort, PacketTransmission<*>>()

    val random = Random()

    fun newSequential(): UShort {
        var seq: UShort
        do {
            // Random ushort
            seq = random.randInt(0, 65535 + 1).toUShort()
        } while (responses.containsKey(seq))
        return seq
    }

    fun processOutgoingTransmission(transmission: PacketTransmission<*>): ByteArray {
        val reaction = transmission.reaction
        val sequential = transmission.respondTo?.sequantial?.inc() ?: newSequential()
        val nextSequential = sequential.inc()
        val packet = transmission.packet.let {
            it.sequantial = sequential
            it.serialize()
        }

        responses[nextSequential] = transmission

        reaction.timeouts.forEach {
            val timeout = it.key
            val task = scheduler.runTaskLater(timeout) {

                // Check that we haven't yet received a packet
                // If the packet came just in time of timeout we don't want to invoke timeout
                if (transmission.received) return@runTaskLater

                // Check if the talker is still open
                if (!talker.isOpen) return@runTaskLater


                transmission.invokeTimeout(timeout, talker)
                responses.remove(nextSequential)
                transmission.timeoutTasks.remove(timeout)
            }
            transmission.timeoutTasks[timeout] = task
        }

        return packet
    }


    suspend fun processIncomingPacket(bytes: ByteArray): IncomingContext<dev.nikdekur.nexushub.packet.Packet>? {
        var seq: UShort? = null
        var response: PacketTransmission<*>? = null
        try {
            val deserializer = PacketDeserializer(bytes)
            val packetId = deserializer.readByte().toUByte()
            seq = deserializer.readShort().toUShort()

            response = responses.remove(seq)
            response?.received = true

            deserializer.focusData()

            val instance = newInstanceFromId(packetId) ?: return null
            instance.deserialize(deserializer)

            instance.sequantial = seq

            response?.processReceived(talker, instance)

            return HandlerContext.Receive(talker, instance, response != null)


        } catch (e: Exception) {
            response?.invokeException(talker, e)
            e.printStackTrace()
            return null
        } finally {
            seq?.let(responses::remove)
        }
    }
}