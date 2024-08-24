/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.access

import dev.nikdekur.ndkore.ext.info
import dev.nikdekur.ndkore.ext.log
import dev.nikdekur.ndkore.ext.warn
import dev.nikdekur.ndkore.service.dependencies
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.access.AccessService.ReceiveResult
import dev.nikdekur.nexushub.auth.AuthenticationService
import dev.nikdekur.nexushub.auth.AuthenticationService.AuthResult.AccountNotFound
import dev.nikdekur.nexushub.auth.AuthenticationService.AuthResult.NodeAlreadyExists
import dev.nikdekur.nexushub.auth.AuthenticationService.AuthResult.NodeAtAddressAlreadyExists
import dev.nikdekur.nexushub.auth.AuthenticationService.AuthResult.NodeNameInvalid
import dev.nikdekur.nexushub.auth.AuthenticationService.AuthResult.Success
import dev.nikdekur.nexushub.auth.AuthenticationService.AuthResult.WrongCredentials
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.network.talker.sendPacket
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.packet.`in`.PacketHeartbeat
import dev.nikdekur.nexushub.packet.`in`.PacketHello
import dev.nikdekur.nexushub.packet.out.PacketReady
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.ratelimit.RateLimitService
import dev.nikdekur.nexushub.talker.ClientTalker
import dev.nikdekur.nexushub.util.CloseCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

class AccessServiceImpl(
    override val app: NexusHubServer
) : AccessService {

    override val dependencies = dependencies {
        after(DataSetService::class)
    }

    val logger = LoggerFactory.getLogger(javaClass)

    val nodesService: NodesService by inject()
    val rateLimitService: RateLimitService by inject()
    val authService: AuthenticationService by inject()
    val protectionService: ProtectionService by inject()

    lateinit var scope: CoroutineScope


    override fun onEnable() {
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    }

    override fun onDisable() {
        scope.cancel()
    }

    override suspend fun receiveData(talker: ClientTalker, data: ByteArray): ReceiveResult {
        if (!rateLimitService.acquire(talker)) {
            talker.closeWithBlock(CloseCode.RATE_LIMITED)
            return ReceiveResult.RateLimited
        }

        // Run as new coroutine to avoid blocking and handle multiple requests at once
        scope.launch {
            val context = talker.receive(data) ?: return@launch

            // Drop a packet if it's a response.
            // All responses are handled by code sending the request
            if (context.isResponse) return@launch

            val packet = context.packet
            val logLevel = when (packet) {
                is PacketHeartbeat -> Level.TRACE
                else -> Level.DEBUG
            }

            val address = talker.address

            logger.log(logLevel) {
                "[$address] Received packet: ${context.packet}"
            }

            try {
                // Authenticated node required
                val node = nodesService.getNode(talker)
                if (node == null) {
                    talker.closeWithBlock(CloseCode.NOT_AUTHENTICATED)
                    return@launch
                }

                node.processPacket(context)
            } catch (e: Exception) {
                logger.warn(e) { "Error while processing packet" }
                e.printStackTrace()
            }
        }

        return ReceiveResult.Ok
    }


    override suspend fun onReady(talker: ClientTalker) {
        val address = talker.address
        val packetHello = PacketHello()

        logger.info("[$address] Waiting for authentication packet")
        talker.sendPacket<Unit>(packetHello) {
            timeout(10.seconds) {
                talker.closeWithBlock(CloseCode.TIMEOUT, "No authentication packet received in time")
                return@timeout
            }

            receive<PacketAuth> {
                logger.info { "Authenticating node: ${packet.node}" }
                val result = authService.authenticate(talker, packet)
                when (result) {
                    is Success -> {
                        val node = result.node
                        logger.info("[$address] Authenticated successfully")

                        val ready = PacketReady(nodesService.pingInterval.inWholeMilliseconds)
                        node.sendPacket(ready)

                        // Wait for the job to finish
                        // job.join()

                        node.close(CloseCode.NORMAL, "Done")

                        return@receive
                    }

                    AccountNotFound -> {
                        protectionService.imitateEncryption()
                        talker.closeWithBlock(CloseCode.WRONG_CREDENTIALS)
                    }

                    WrongCredentials -> {
                        talker.closeWithBlock(CloseCode.WRONG_CREDENTIALS)
                    }

                    NodeNameInvalid -> {
                        talker.closeWithBlock(CloseCode.INVALID_DATA, "Provided node name is not valid")
                    }

                    NodeAlreadyExists -> {
                        talker.closeWithBlock(CloseCode.NODE_ALREADY_EXISTS, "Node with this id already exists")
                    }

                    NodeAtAddressAlreadyExists -> {
                        talker.closeWithBlock(
                            CloseCode.NODE_ALREADY_EXISTS,
                            "Node with this address already exists"
                        )
                    }
                }
            }

            receive {
                talker.closeWithBlock(CloseCode.UNEXPECTED_BEHAVIOUR, "Was expecting: PacketAuth")
            }
        }.await()
    }
}