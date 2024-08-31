/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.transmission

import dev.nikdekur.ndkore.ext.smartAwait
import dev.nikdekur.nexushub.network.dsl.HandlerContext
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.dsl.ReceiveHandler
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.packet.Packet
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.TimeoutException
import kotlin.time.Duration

/**
 * Represents a packet transmission
 *
 * @param packet The packet to be transmitted
 * @param reaction The reaction to the packet
 */
data class RuntimePacketTransmission<R>(
    override val talker: Talker,
    override val packet: Packet,
    override val reaction: PacketReaction<R>,
    override val respondTo: UShort?
) : PacketTransmission<R> {

    var receivedPacket: Packet? = null

    override var received = false

    val result = CompletableDeferred<R>()

    override suspend fun onException(talker: Talker, e: Exception) {
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

    override suspend fun onReceive(talker: Talker, packet: Packet) {
        received = true

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
            onException(talker, e)
        }
    }

    internal suspend inline fun <T : Packet> invokeReceiveHandler(
        talker: Talker,
        handler: ReceiveHandler<T, R>,
        packet: T
    ) {
        try {
            val context = HandlerContext.Receive<T, R>(talker, packet, true)
            result.complete(handler.invoke(context))
        } catch (e: Exception) {
            onException(talker, e)
        }
    }


    override suspend fun onTimeout(timeout: Duration) {
        val context = HandlerContext.Timeout<R>(talker, packet)
        val timeoutHandler = reaction.timeouts[timeout]
        if (timeoutHandler == null) {
            result.completeExceptionally(TimeoutException("No timeout response found for $timeout, but it was triggered"))
        } else {
            result.complete(timeoutHandler.invoke(context))
        }
    }

    override suspend fun await() = result.smartAwait()
}