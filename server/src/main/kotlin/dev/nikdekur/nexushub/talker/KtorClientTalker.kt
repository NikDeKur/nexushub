/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.talker

import dev.nikdekur.ndkore.ext.debug
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.PacketManager
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.service.NexusHubComponent
import dev.nikdekur.nexushub.util.CloseCode
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import org.slf4j.LoggerFactory

class KtorClientTalker(
    override val app: NexusHubServer,
    val websocket: DefaultWebSocketServerSession,
    override val address: Address
) : ClientTalker, NexusHubComponent {

    val logger = LoggerFactory.getLogger(javaClass)

    val packetManager = PacketManager(this, Dispatchers.IO)

    @OptIn(DelicateCoroutinesApi::class)
    override val isOpen: Boolean
        get() = websocket.closeReason.isActive && (!websocket.outgoing.isClosedForSend && !websocket.incoming.isClosedForReceive) && !isBlocked

    override var isBlocked: Boolean = false

    override suspend fun send(transmission: PacketTransmission<*>) {
        val bytes = packetManager.processOutgoingTransmission(transmission)
        logger.debug { "[$address] Sending packet ${transmission.packet}" }
        websocket.send(bytes)
    }

    override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
        return packetManager.processIncomingPacket(data)
    }

    override suspend fun closeWithBlock(code: CloseCode, reason: String) {
        isBlocked = true
        close(code, reason)
    }

    override suspend fun close(code: CloseCode, comment: String) {
        // talkersService.removeTalker(address)
        websocket.close(CloseReason(code.code, comment))
    }


    override fun equals(other: Any?): Boolean {
        if (other !is KtorClientTalker) return false
        return this.address == other.address
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }

    override fun toString(): String {
        return "KtorTalker(address='$address')"
    }
}