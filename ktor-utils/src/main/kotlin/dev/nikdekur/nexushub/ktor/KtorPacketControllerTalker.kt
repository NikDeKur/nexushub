/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:OptIn(DelicateCoroutinesApi::class)

package dev.nikdekur.nexushub.ktor

import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.CloseCode
import dev.nikdekur.nexushub.network.RuntimePacketController
import dev.nikdekur.nexushub.network.talker.PacketControllerTalker
import dev.nikdekur.nexushub.network.timeout.TimeoutService
import io.ktor.websocket.*
import kotlinx.coroutines.DelicateCoroutinesApi

open class KtorPacketControllerTalker(
    val websocket: DefaultWebSocketSession,
    override val address: Address,
    timeoutService: TimeoutService
) : PacketControllerTalker(address) {

    override val packetController = RuntimePacketController(this, timeoutService)

    override val isOpen: Boolean
        get() = websocket.closeReason.isActive && (!websocket.outgoing.isClosedForSend && !websocket.incoming.isClosedForReceive) && super.isOpen

    override suspend fun send(data: ByteArray) {
        websocket.send(data)
    }

    override suspend fun close(code: CloseCode, comment: String) {
        websocket.close(CloseReason(code.code, comment))
    }
}