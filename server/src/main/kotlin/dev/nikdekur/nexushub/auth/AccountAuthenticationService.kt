/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth

import dev.nikdekur.ndkore.ext.delay
import dev.nikdekur.ndkore.ext.info
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.auth.account.AccountsService
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.node.ClientNode
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.talker.ClientTalker
import dev.nikdekur.nexushub.util.CloseCode
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class AccountAuthenticationService(
    override val app: NexusHubServer
) : NexusHubService, AuthenticationService {

    val logger = LoggerFactory.getLogger(javaClass)

    val nodesService: NodesService by inject()
    val protectionService: ProtectionService by inject()
    val accountsService: AccountsService by inject()

    override suspend fun executeAuthenticatedPacket(talker: ClientTalker, context: IncomingContext<Packet>) {
        // Authenticated node required
        val node = nodesService.getAuthenticatedNode(talker)
        if (node == null) {
            talker.closeWithBlock(CloseCode.NOT_AUTHENTICATED)
            return
        }

        node.processAuthenticatedPacket(context)
    }

    override suspend fun processAuth(talker: ClientTalker, packet: PacketAuth) {
        logger.info { "Authenticating node: ${packet.node}" }

        val account = accountsService.getAccount(packet.login)
        if (account == null) {
            // Imitate hashing delay to hacker think login exists
            logger.info { "Account not found: ${packet.login}" }
            delay(protectionService.averageEncryptionTime())
            talker.closeWithBlock(CloseCode.WRONG_CREDENTIALS)
            return
        }

        val isCorrect = accountsService.matchPassword(account.password, packet.password)
        if (!isCorrect) {
            logger.info { "Incorrect password for account: ${packet.login}" }
            talker.closeWithBlock(CloseCode.WRONG_CREDENTIALS)
            return
        }

        val nodeStr = packet.node
        if (!isValidNodeName(nodeStr)) {
            talker.closeWithBlock(CloseCode.INVALID_DATA, "Provided node name is not valid")
            return
        }

        if (nodesService.isNodeExists(talker)) {
            talker.closeWithBlock(CloseCode.NODE_ALREADY_EXISTS, "Node at exact same address already exists")
            return
        }

        if (nodesService.isNodeExists(nodeStr)) {
            talker.closeWithBlock(CloseCode.NODE_ALREADY_EXISTS, "Node with this id already exists")
            return
        }

        val node = ClientNode(talker, nodeStr, account)
        nodesService.addNode(node)

        logger.info { "Authenticated node: $node" }
    }

    val validNodePattern = Regex("[a-zA-Z0-9_]+")
    fun isValidNodeName(node: String): Boolean {
        return validNodePattern.matches(node) && node.length <= 32 && node.length >= 4
    }


}