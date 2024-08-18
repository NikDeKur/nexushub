/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:OptIn(DelicateCoroutinesApi::class)

package dev.nikdekur.nexushub.connection

import dev.nikdekur.nexushub.network.PacketManager
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.util.CloseCode
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.withContext
import java.util.*

class ServerTalker(
    val websocket: DefaultClientWebSocketSession,
    val networkDispatcher: CoroutineDispatcher
) : Talker {


    override val addressHash = websocket.call.request.url.let {
        Objects.hash(it.host, it.port)
    }

    override val addressStr = websocket.call.request.url.let {
        "${it.host}:${it.port}"
    }


    val packetManager = PacketManager(this, networkDispatcher)

    override val isOpen: Boolean
        get() = websocket.closeReason.isActive && (!websocket.outgoing.isClosedForSend && !websocket.incoming.isClosedForReceive)

    val isClosed: Boolean
        get() = !websocket.closeReason.isActive

    override suspend fun send(transmission: PacketTransmission<*>) {
        withContext(networkDispatcher) {
            val bytes = packetManager.processOutgoingTransmission(transmission)
            websocket.send(bytes)
        }
    }

    override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
        return packetManager.processIncomingPacket(data)
    }

    override suspend fun close(code: CloseCode, comment: String) {
        websocket.close(CloseReason(code.code, comment))
    }
}