/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.connection.handler

import kotlinx.coroutines.flow.Flow
import dev.nikdekur.nexushub.connection.gateway.GatewayConfiguration
import dev.nikdekur.nexushub.connection.retry.Retry
import dev.nikdekur.nexushub.event.Event
import dev.nikdekur.nexushub.event.NetworkEvent
import dev.nikdekur.nexushub.packet.`in`.PacketAuth

internal class AuthenticationHandler(
    flow: Flow<Event>,
    private val reconnectRetry: Retry
) : Handler(flow, "AuthenticationHandler") {

    lateinit var configuration: GatewayConfiguration

    override fun start() {
        on<NetworkEvent.Hello> {
            logger.info("Authenticating...")
            reconnectRetry.reset() // connected and read without problems, resetting retry counter
            it.respond(configuration.auth)
        }
    }
}


internal inline val GatewayConfiguration.auth: PacketAuth
    get() = PacketAuth(login, password, node)