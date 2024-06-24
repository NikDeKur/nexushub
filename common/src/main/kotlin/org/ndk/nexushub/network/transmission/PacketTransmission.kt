package org.ndk.nexushub.network.transmission

import dev.nikdekur.ndkore.ext.smartAwait
import dev.nikdekur.ndkore.scheduler.SchedulerTask
import kotlinx.coroutines.CompletableDeferred
import org.ndk.nexushub.network.dsl.HandlerContext
import org.ndk.nexushub.network.dsl.PacketReaction
import org.ndk.nexushub.network.dsl.ReceiveHandler
import org.ndk.nexushub.network.talker.Talker
import org.ndk.nexushub.packet.Packet
import java.util.concurrent.TimeoutException

/**
 * Represents a packet transmission
 *
 * @param packet The packet to be transmitted
 * @param reaction The reaction to the packet
 */
data class PacketTransmission<R>(
    val packet: Packet,
    val reaction: PacketReaction<R>
) {

    var respondTo: Packet? = null

    var receivedPacket: Packet? = null

    // PacketManager change to true
    var received = false

    val result = CompletableDeferred<R>()

    val timeoutTasks = HashMap<Long, SchedulerTask>()

    internal suspend fun invokeException(talker: Talker, e: Exception) {
        try {
            val context = HandlerContext.Exception<R>(talker, packet, e)
            val exHandler = reaction.onException
            if (exHandler == null) {
                result.completeExceptionally(e)
            } else {
                result.complete(exHandler.invoke(context))
            }
        } catch (e: Exception) {
            result.completeExceptionally(e)
            throw e
        }
    }

    internal suspend fun invokeReceive(talker: Talker, packet: Packet) {
        receivedPacket = packet
        val typedHandler = reaction.receiveHandles[packet::class.java]
        try {
            if (typedHandler != null)
                invokeReceiveHandler(talker, typedHandler, packet)
            else {
                val nullHandler = reaction.receiveHandles[null]
                if (nullHandler != null)
                    invokeReceiveHandler(talker, nullHandler, packet)
            }
        } catch (e: Exception) {
            invokeException(talker, e)
        }
    }

    internal suspend inline fun <T : Packet> invokeReceiveHandler(talker: Talker, handler: ReceiveHandler<T, R>, packet: T) {
        try {
            val context = HandlerContext.Receive<T, R>(talker, packet, true)
            result.complete(handler.invoke(context))
        } catch (e: Exception) {
            invokeException(talker, e)
        }
    }


    suspend fun invokeTimeout(timeout: Long, talker: Talker) {
        try {
            val context = HandlerContext.Timeout<R>(talker, packet)
            val timeoutHandler = reaction.timeouts[timeout]
            if (timeoutHandler == null) {
                result.completeExceptionally(TimeoutException("No timeout response found for $timeout, but it was triggered"))
            } else {
                result.complete(timeoutHandler.invoke(context))
            }
        } catch (e: Exception) {
            invokeException(talker, e)
        }
    }


    suspend fun processReceived(talker: Talker, packet: Packet) {
        timeoutTasks.values.forEach { it.cancel() }
        timeoutTasks.clear()

        invokeReceive(talker, packet)
    }



    suspend fun await(): R {
        return result.smartAwait()
    }
}