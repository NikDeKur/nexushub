package org.ndk.nexushub.network

import dev.nikdekur.ndkore.ext.randInt
import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import kotlinx.coroutines.CoroutineDispatcher
import org.ndk.nexushub.network.dsl.HandlerContext
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.network.transmission.PacketTransmission
import org.ndk.nexushub.packet.serialize.PacketDeserializer
import org.ndk.nexushub.packet.type.PacketTypes.newInstanceFromId
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


    suspend fun processIncomingPacket(bytes: ByteArray): IncomingContext<org.ndk.nexushub.packet.Packet>? {
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