package org.ndk.nexushub.network

import org.ndk.global.scheduler.Scheduler
import org.ndk.nexushub.network.dsl.HandlerContext
import org.ndk.nexushub.network.dsl.IncomingContext
import org.ndk.nexushub.network.packet.Packet
import org.ndk.nexushub.network.packet.serialize.PacketDeserializer
import org.ndk.nexushub.network.packet.type.PacketTypes.newInstanceFromId
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.network.transmission.PacketTransmission

open class PacketManager(val talker: Talker, val scheduler: Scheduler) {

    val responses = HashMap<UByte, PacketTransmission<*>>()

    val randomSequentials = List(0x254) { it.toUByte() }
    fun newSequential(): UByte {
        var seq: UByte
        do {
            seq = randomSequentials.random()
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

        val timeout = reaction.timeout
        if (timeout != null) {
            transmission.timeoutTask = scheduler.runTaskLater(timeout) {
                if (!transmission.received) {
                    transmission.invokeTimeout(talker)
                    responses.remove(nextSequential)
                }
            }
        }

        return packet
    }


    suspend fun processIncomingPacket(bytes: ByteArray): IncomingContext<Packet>? {
        var seq: UByte? = null
        var response: PacketTransmission<*>? = null
        try {
            val deserializer = PacketDeserializer(bytes)
            val id = deserializer.readByte().toUByte()
            seq = deserializer.readByte().toUByte()

            response = responses.remove(seq)
            response?.received = true

            deserializer.focusData()

            val instance = newInstanceFromId(id) ?: return null
            instance.deserialize(deserializer)

            instance.sequantial = seq

            response?.processReceived(talker, instance)

            return HandlerContext.Incoming(talker, instance)


        } catch (e: Exception) {
            response?.invokeException(talker, e)
            e.printStackTrace()
            return null
        } finally {
            seq?.let(responses::remove)
        }
    }
}