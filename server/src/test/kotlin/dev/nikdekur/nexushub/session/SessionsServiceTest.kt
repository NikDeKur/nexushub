/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.session

import dev.nikdekur.nexushub.data.Leaderboard
import dev.nikdekur.nexushub.data.LeaderboardEntry
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.dsl.PacketReaction
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.node.Node
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.scope.Scope
import dev.nikdekur.nexushub.util.CloseCode
import dev.nikdekur.nexushub.util.NexusData
import org.junit.jupiter.api.Test
import java.util.function.Predicate
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

/**
 * We guarantee that the [service] will be the new instance of [SessionsService] for each test.
 */
interface SessionsServiceTest {

    val service: SessionsService

    val fakeScope
        get() = object : Scope {
            override val id: String = "scope"
            override suspend fun loadData(holderId: String): NexusData {
                TODO("Not yet implemented")
            }

            override suspend fun setData(holderId: String, data: NexusData) {
                TODO("Not yet implemented")
            }

            override suspend fun getLeaderboard(
                path: String,
                startFrom: Int,
                limit: Int
            ): Leaderboard {
                TODO("Not yet implemented")
            }

            override suspend fun getTopPosition(
                holderId: String,
                field: String
            ): LeaderboardEntry? {
                TODO("Not yet implemented")
            }
        }

    fun fakeNode(id: String = "node") = object : Node {
        override val id: String = id
        override fun isAlive(deadInterval: Duration): Boolean {
            TODO("Not yet implemented")
        }

        override suspend fun processPacket(context: IncomingContext<out Packet>) {
            TODO("Not yet implemented")
        }

        override suspend fun requestScopeSync(scope: Scope) {
            TODO("Not yet implemented")
        }

        override val address: Address
            get() = TODO("Not yet implemented")
        override val isOpen: Boolean
            get() = TODO("Not yet implemented")

        override suspend fun <R> send(
            packet: Packet,
            reaction: PacketReaction.Builder<R>.() -> Unit,
            respondTo: UShort?
        ): PacketTransmission<R> {
            TODO("Not yet implemented")
        }

        override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
            TODO("Not yet implemented")
        }

        override suspend fun <T : Packet> wait(
            packetClass: Class<T>,
            condition: Predicate<T>
        ): IncomingContext<T> {
            TODO("Not yet implemented")
        }

        override suspend fun close(code: CloseCode, comment: String) {
            TODO("Not yet implemented")
        }

        override val isBlocked: Boolean
            get() = TODO("Not yet implemented")

        override suspend fun closeWithBlock(code: CloseCode, reason: String) {
            TODO("Not yet implemented")
        }
    }


    @Test
    fun `test get non existing session`() {
        val session = service.getExistingSession("scope", "holder")
        assertNull(session)
    }

    @Test
    fun `test get nodes when no nodes`() {
        val nodes = service.getNodes(fakeScope)
        assertTrue(nodes.none())
    }

    @Test
    fun `test has any sessions when no sessions`() {
        val hasAnySessions = service.hasAnySessions(fakeNode())
        assertFalse(hasAnySessions)
    }

    @Test
    fun `test start session`() {
        service.startSession(fakeNode(), fakeScope, "holder")
    }


    @Test
    fun `test start and get existing session`() {
        val scope = fakeScope
        service.startSession(fakeNode(), scope, "holder")
        val session = service.getExistingSession(scope.id, "holder")
        assertNotNull(session)
    }

    @Test
    fun `test start and get existing session with different holder`() {
        val scope = fakeScope
        service.startSession(fakeNode(), scope, "holder")
        val session = service.getExistingSession(scope.id, "holder2")
        assertNull(session)
    }

    @Test
    fun `test start and get existing session with different scope`() {
        val scope = fakeScope
        service.startSession(fakeNode(), scope, "holder")
        val session = service.getExistingSession("scope2", "holder")
        assertNull(session)
    }

    @Test
    fun `test start and get existing session with different scope and holder`() {
        val scope = fakeScope
        service.startSession(fakeNode(), scope, "holder")
        val session = service.getExistingSession("scope2", "holder2")
        assertNull(session)
    }

    @Test
    fun `test start and get existing session with multiple sessions`() {
        val scope = fakeScope
        service.startSession(fakeNode(), scope, "holder")
        service.startSession(fakeNode(), scope, "holder2")
        val session = service.getExistingSession(scope.id, "holder")
        assertNotNull(session)
        val session2 = service.getExistingSession(scope.id, "holder2")
        assertNotNull(session2)
    }


    @Test
    fun `stop non existing session`() {
        service.stopSession("scope", "holder")
    }

    @Test
    fun `test start and stop session`() {
        val scope = fakeScope
        val holderId = "holder"
        service.startSession(fakeNode(), scope, holderId)
        assertNotNull(service.getExistingSession(scope.id, holderId))
        service.stopSession(scope.id, holderId)
        assertNull(service.getExistingSession(scope.id, holderId))
    }

    @Test
    fun `test start and stop session with different holder`() {
        val scope = fakeScope
        val holderId = "holder"
        service.startSession(fakeNode(), scope, holderId)
        assertNotNull(service.getExistingSession(scope.id, holderId))
        service.stopSession(scope.id, "holder2")
        assertNotNull(service.getExistingSession(scope.id, holderId))
    }

    @Test
    fun `test start and stop session with different scope`() {
        val scope = fakeScope
        val holderId = "holder"
        service.startSession(fakeNode(), scope, holderId)
        assertNotNull(service.getExistingSession(scope.id, holderId))
        service.stopSession("scope2", holderId)
        assertNotNull(service.getExistingSession(scope.id, holderId))
    }

    @Test
    fun `test start and stop session with different scope and holder`() {
        val scope = fakeScope
        val holderId = "holder"
        service.startSession(fakeNode(), scope, holderId)
        assertNotNull(service.getExistingSession(scope.id, holderId))
        service.stopSession("scope2", "holder2")
        assertNotNull(service.getExistingSession(scope.id, holderId))
    }

    @Test
    fun `stop all sessions for node`() {
        service.stopAllSessions(fakeNode())
    }

    @Test
    fun `test start and stop all sessions for node`() {
        val scope = fakeScope
        val holderId = "holder"
        val node = fakeNode()

        service.startSession(node, scope, holderId)
        assertNotNull(service.getExistingSession(scope.id, holderId))

        service.stopAllSessions(node)
        assertNull(service.getExistingSession(scope.id, holderId))
    }

    @Test
    fun `test start and stop all sessions for node with multiple sessions`() {
        val scope = fakeScope
        val holderId = "holder"
        val holderId2 = "holder2"
        val node = fakeNode()

        service.startSession(node, scope, holderId)
        service.startSession(node, scope, holderId2)
        assertNotNull(service.getExistingSession(scope.id, holderId))
        assertNotNull(service.getExistingSession(scope.id, holderId2))

        service.stopAllSessions(node)
        assertNull(service.getExistingSession(scope.id, holderId))
        assertNull(service.getExistingSession(scope.id, holderId2))
    }


    @Test
    fun `test start and stop all sessions for different node`() {
        val scope = fakeScope
        val holderId = "holder"
        val node1 = fakeNode()
        val node2 = fakeNode("node2")

        service.startSession(node1, scope, holderId)
        assertNotNull(service.getExistingSession(scope.id, holderId))

        service.stopAllSessions(node2)
        assertNotNull(service.getExistingSession(scope.id, holderId))
    }


}