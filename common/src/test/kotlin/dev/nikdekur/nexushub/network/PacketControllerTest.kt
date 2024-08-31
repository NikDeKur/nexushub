/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.packet.PacketOk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

interface PacketControllerTest {

    fun newController(): PacketController

    @Test
    fun `test new transmission`() = runBlocking {
        val controller = newController()
        val packet = PacketOk("test")
        val reaction = PacketReaction.Builder<Unit>().build()
        val respondTo = null
        val transmission = controller.newTransmission(packet, reaction, respondTo)
        assertEquals(packet, transmission.packet)
        assertEquals(reaction, transmission.reaction)
        assertEquals(respondTo, transmission.respondTo)
    }

    @Test
    fun `test sending packet`() = runBlocking {
        val controller1 = newController()
        val controller2 = newController()

        val packet = PacketOk("test")

        val reaction = PacketReaction.EMPTY
        val transmission = controller1.newTransmission(packet, reaction, null)
        val bytes = controller1.processSending(transmission)
        val received = controller2.processReceiving(bytes)
        assertEquals(packet, received?.packet)
    }

    @Test
    fun `test send and respond with reaction`(): Unit = runBlocking {
        val controller1 = newController()
        val controller2 = newController()

        val reaction = PacketReaction.Builder<String>()
            .apply {
                receive<PacketOk> {
                    return@receive packet.message
                }
            }
            .build()

        val transmission = controller1.newTransmission(PacketOk("test"), reaction, null)
        val bytes = controller1.processSending(transmission)

        val receiveContext = controller2.processReceiving(bytes)
        assertNotNull(receiveContext)
        val responseTransmission =
            controller2.newTransmission(PacketOk("yes!"), PacketReaction.EMPTY, receiveContext.packet.sequantial)
        val responseBytes = controller2.processSending(responseTransmission)

        controller1.processReceiving(responseBytes)

        val response = transmission.await()
        assertEquals("yes!", response)

        assertNotNull(receiveContext)
    }
}