/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.behavior

import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.TestNexusHubServer
import dev.nikdekur.nexushub.access.AccessService
import dev.nikdekur.nexushub.boot.Environment
import dev.nikdekur.nexushub.dataset.map.MapDataSetService
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.RuntimePacketController
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.talker.PacketControllerTalker
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.network.talker.TestTalker
import dev.nikdekur.nexushub.network.talker.receive
import dev.nikdekur.nexushub.network.talker.wait
import dev.nikdekur.nexushub.network.timeout.SchedulerTimeoutService
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.PacketAuth
import dev.nikdekur.nexushub.packet.PacketHello
import dev.nikdekur.nexushub.protection.none.NoneProtectionService
import dev.nikdekur.nexushub.storage.runtime.RuntimeStorageService
import dev.nikdekur.nexushub.util.CloseCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Test
import java.util.function.Predicate
import kotlin.properties.Delegates
import kotlin.test.assertNotNull

class MainBehaviorTest {

    val environment = object : Environment {
        override fun getValue(key: String): String? {
            return null
        }

        override fun requestValue(key: String, description: String): String? {
            return when (key) {
                "root_password" -> "Password1"
                else -> null
            }
        }
    }

    val server = object : TestNexusHubServer() {
        override val environment = this@MainBehaviorTest.environment

        override fun buildDataSetService() = MapDataSetService(this, mapOf())
        override fun buildStorageService() = RuntimeStorageService(this)
        override fun buildProtectionService() = NoneProtectionService(this)
    }.also {
        it.start()
    }

    @Test
    fun `test authentication`(): Unit = runTest(StandardTestDispatcher()) {

        val accessService by server.inject<AccessService>()

        val timeoutService = SchedulerTimeoutService(
            CoroutineScheduler.fromSupervisor(Dispatchers.IO)
        )

        val serverAddress = Address("127.0.0.1", 8085)
        val clientAddress = Address("127.0.0.1", 8080)

        var clientToServerTalker by Delegates.notNull<TestTalker>()
        var serverToTalker by Delegates.notNull<Talker>()


        // Client START
        clientToServerTalker = object : PacketControllerTalker(serverAddress), TestTalker {
            override val packetController = RuntimePacketController(this, timeoutService)

            val queue = ArrayDeque<IncomingContext<Packet>>()

            override suspend fun send(data: ByteArray) {
                // Send data to server
                accessService.receiveData(serverToTalker, data)
            }

            override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
                return super.receive(data).also {
                    it?.let(queue::add)
                }
            }


            override val isOpen: Boolean
                get() = true

            override suspend fun close(code: CloseCode, comment: String) {
                println("[CLIENT] Closing server connection: $code, `$comment`")
            }

            override fun <T : Packet> receive(clazz: Class<T>, condition: Predicate<T>): IncomingContext<T>? {
                val context = queue.removeFirstOrNull() ?: return null
                if (!clazz.isInstance(context.packet)) return null
                if (!condition.test(clazz.cast(context.packet))) return null
                @Suppress("UNCHECKED_CAST")
                return context as IncomingContext<T>
            }
        }
        // Client END


        // Server START
        serverToTalker = object : PacketControllerTalker(clientAddress) {
            override val packetController = RuntimePacketController(this, timeoutService)

            override var isBlocked: Boolean = false

            override val isOpen: Boolean
                get() = !isBlocked

            override suspend fun closeWithBlock(code: CloseCode, reason: String) {
                isBlocked = true
                close(code, reason)
            }

            override suspend fun send(data: ByteArray) {
                // Send data to the client
                clientToServerTalker.receive(data)
            }

            override suspend fun close(code: CloseCode, comment: String) {
                println("[SERVER] Closing client connection: $code, `$comment`")
            }
        }
        // Server END

        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        val def = CompletableDeferred<Unit>()

        scope.launch {
            println("Server is starting")
            def.complete(Unit)
            accessService.onReady(serverToTalker)
        }

        println("Waiting for server to be ready")
        def.await()
        println("Server is ready")

        val receive = clientToServerTalker.receive<PacketHello>()
        assertNotNull(receive)
        println("Received hello packet: $receive")


        withTimeoutOrNull(100) {
            clientToServerTalker.wait<PacketHello>()
        } ?: error("Server did not send hello packet")

        val fakeAuthPacket = PacketAuth("test", "Password1", "test1")
        clientToServerTalker.send<Unit>(fakeAuthPacket)

    }
}