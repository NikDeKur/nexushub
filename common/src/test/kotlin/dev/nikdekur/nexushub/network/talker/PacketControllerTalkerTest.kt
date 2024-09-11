/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.talker

import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.CloseCode
import dev.nikdekur.nexushub.network.RuntimePacketController
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.timeout.SchedulerTimeoutService
import dev.nikdekur.nexushub.packet.Packet
import kotlinx.coroutines.Dispatchers
import java.util.function.Predicate

class PacketControllerTalkerTest : TalkerTest {

    val timeoutService = SchedulerTimeoutService(
        CoroutineScheduler.fromSupervisor(Dispatchers.IO)
    )

    override fun newTalker(
        address: Address,
        sendCallback: suspend (ByteArray) -> Unit,
        receiveCallback: suspend (IncomingContext<Packet>?) -> Unit,
        isOpen: () -> Boolean,
        onClose: () -> Unit
    ): TestTalker {

        return object : PacketControllerTalker(address), TestTalker {
            override val packetController = RuntimePacketController(this, timeoutService)

            val queue = ArrayDeque<IncomingContext<Packet>>()

            override suspend fun send(data: ByteArray) {
                sendCallback(data)
            }

            override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
                return super.receive(data).also {
                    receiveCallback(it)
                    it?.let(queue::add)
                }
            }

            override fun <T : Packet> receive(clazz: Class<T>, condition: Predicate<T>): IncomingContext<T>? {
                val context = queue.removeFirstOrNull() ?: return null
                if (!clazz.isInstance(context.packet)) return null
                if (!condition.test(clazz.cast(context.packet))) return null
                @Suppress("UNCHECKED_CAST")
                return context as IncomingContext<T>
            }

            override val isOpen: Boolean
                get() = isOpen()

            override suspend fun close(code: CloseCode, comment: String) {
                onClose()
            }
        }
    }
}