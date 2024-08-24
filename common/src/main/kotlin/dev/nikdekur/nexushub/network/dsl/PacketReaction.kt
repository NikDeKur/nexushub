/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.dsl

import dev.nikdekur.nexushub.packet.Packet
import kotlin.time.Duration

data class PacketReaction<R>(
    val receiveHandles: MutableMap<Class<out Packet>?, ReceiveHandler<Packet, R>>,
    val timeouts: MutableMap<Duration, TimeoutHandler<R>>,
    val onException: ExceptionHandler<R>?
) {


    class Builder<R> {
        val receiveHandles = HashMap<Class<out Packet>?, ReceiveHandler<out Packet, R>>()
        val timeouts = HashMap<Duration, TimeoutHandler<R>>()
        var onException: ExceptionHandler<R>? = null


        @PacketReactionDSL
        @Suppress("UNCHECKED_CAST")
        fun <T : Packet> receive(clazz: Class<out T>, block: ReceiveHandler<T, R>) {
            receiveHandles[clazz] = block as ReceiveHandler<Packet, R>
        }

        @PacketReactionDSL
        inline fun <reified T : Packet> receive(noinline block: ReceiveHandler<T, R>) = receive(T::class.java, block)

        @PacketReactionDSL
        @JvmName("receiveDefault")
        fun receive(block: ReceiveHandler<Packet, R>) {
            receiveHandles[null] = block
        }

        @PacketReactionDSL
        fun timeout(timeout: Duration, block: TimeoutHandler<R>) {
            timeouts[timeout] = block
        }

        @PacketReactionDSL
        fun throwOnTimeout(timeout: Duration) {
            timeouts[timeout] = {
                throw Exception("Timeout while waiting for packet")
            }
        }

        @PacketReactionDSL
        fun exception(block: ExceptionHandler<R>?) {
            onException = block
        }

        fun build(): PacketReaction<R> {
            return PacketReaction(
                receiveHandles,
                timeouts,
                onException
            )
        }
    }

}