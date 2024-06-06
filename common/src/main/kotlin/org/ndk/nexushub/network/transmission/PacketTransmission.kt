package org.ndk.nexushub.network.transmission

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.ndk.global.scheduler.SchedulerTask
import org.ndk.klib.smartAwait
import org.ndk.nexushub.network.dsl.HandlerContext
import org.ndk.nexushub.network.dsl.PacketReaction
import org.ndk.nexushub.network.dsl.ReceiveHandler
import org.ndk.nexushub.network.packet.Packet
import org.ndk.nexushub.network.talker.Talker
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
    var received = false

    val result = CompletableDeferred<R>()
    val processed: Boolean
        get() = result.isCompleted




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
            val context = HandlerContext.Receive<T, R>(talker, packet)
            result.complete(handler.invoke(context))
        } catch (e: Exception) {
            invokeException(talker, e)
        }
    }


    var timeoutTask: SchedulerTask? = null
    suspend fun invokeTimeout(talker: Talker) {
        try {
            val context = HandlerContext.Timeout<R>(talker, packet)
            val timeoutHandler = reaction.onTimeout
            if (timeoutHandler == null) {
                result.completeExceptionally(TimeoutException("Timeout while waiting for response"))
            } else {
                result.complete(timeoutHandler.invoke(context))
            }
        } catch (e: Exception) {
            invokeException(talker, e)
        }
    }

    /**
     * Process packet receiving
     *
     * Invokes the corresponding receiver handles for the received packet and all-packets handler (null-key).
     *
     * Marks the waiter as received.
     *
     * @param packet The received packet
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun processReceived(talker: Talker, packet: Packet) {
        timeoutTask?.cancel()
        timeoutTask = null

        invokeReceive(talker, packet)

        // invokeReceive should cover everything and lead to result be somehow completed
        markProcessed(result.getCompleted())
    }

    private fun markProcessed(result: R) {
        this.result.complete(result)
    }


    suspend fun await(): R {
        return result.smartAwait()
    }
}