/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.talker

import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.PacketAuth
import dev.nikdekur.nexushub.packet.PacketOk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.properties.Delegates
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

interface TalkerTest {

    fun newTalker(
        address: Address,
        sendCallback: suspend (ByteArray) -> Unit = {},
        receiveCallback: suspend (IncomingContext<Packet>?) -> Unit = {},
        isOpen: () -> Boolean = { true },
        onClose: () -> Unit = {}
    ): TestTalker

    fun newTalkersPair(
        address1: Address,
        address2: Address
    ): Pair<TestTalker, TestTalker> {
        var talker1 by Delegates.notNull<TestTalker>()
        var talker2 by Delegates.notNull<TestTalker>()

        talker1 = newTalker(
            address1,
            sendCallback = { talker2.receive(it) },
        )

        talker2 = newTalker(
            address2,
            sendCallback = { talker1.receive(it) },
        )

        return talker1 to talker2
    }

    @Test
    fun `test send`(): Unit = runBlocking {
        val address = Address("127.0.0.1", 8080)
        val talker = newTalker(address)
        val packet = PacketOk("test")
        val transmission = talker.send<Unit>(packet)
        assertEquals(packet, transmission.packet)
    }

    @Test
    fun `test send and receive`(): Unit = runBlocking {

        // as address always use "localhost" and each time new port
        val (talker1, talker2) = newTalkersPair(
            Address("localhost", 8080),
            Address("localhost", 8081),
        )

        val expectedText = "test"

        val packet = PacketOk(expectedText)
        talker1.send<Unit>(packet)

        val received = talker2.receive<PacketOk>()
        assertNotNull(received, "packet not received")

        assertEquals(expectedText, received.packet.message)
    }


    @Test
    fun `test send, receive and respond`(): Unit = runBlocking {

        // as address always use "localhost" and each time new port
        val (talker1, talker2) = newTalkersPair(
            Address("localhost", 8080),
            Address("localhost", 8081),
        )

        // Test Plan:
        // 1. Send a packet from talker1 to talker2
        // 2. Ensure that talker2 will receive it
        // 3. Respond to talker1 with packet
        // 4. Ensure that talker1 will receive the response

        val packet = PacketAuth("1", "2", "3")
        talker1.send<Unit>(packet)

        val context = talker2.receive<PacketAuth>()
        assertNotNull(context, "packet not received")

        val response = PacketOk("done")
        context.respond<Unit>(response)
        assertEquals(packet, context.packet)

        val received = talker1.receive<PacketOk>()
        assertNotNull(received, "response not received")

        assertEquals("done", received.packet.message)
    }


    @Test
    fun `test send and wait with reaction`(): Unit = runBlocking {
        val (talker1, talker2) = newTalkersPair(
            Address("localhost", 8080),
            Address("localhost", 8081),
        )

        var ok = false

        val packet = PacketAuth("1", "2", "3")
        talker1.send<Unit>(packet) {
            receive<PacketOk> {
                ok = true
            }
        }

        val context = talker2.receive<PacketAuth>()
        assertNotNull(context, "packet not received")

        context.respond<Unit>(PacketOk("done"))

        assertTrue(ok, "reaction not called")
    }
}
