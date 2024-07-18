/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth

import dev.nikdekur.ndkore.ext.info
import dev.nikdekur.nexushub.auth.account.AccountsService
import dev.nikdekur.nexushub.auth.password.EncryptedPassword
import dev.nikdekur.nexushub.auth.password.PasswordEncryptor
import dev.nikdekur.nexushub.koin.NexusHubComponent
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.node.ClientNode
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.`in`.PacketAuth
import dev.nikdekur.nexushub.talker.ClientTalker
import dev.nikdekur.nexushub.util.CloseCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import org.slf4j.LoggerFactory

class AuthenticationServiceImpl : AuthenticationService, NexusHubComponent {

    val logger = LoggerFactory.getLogger(javaClass)

    val nodesService: NodesService by inject()
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
            delay(PasswordEncryptor.averageHashTime())
            talker.closeWithBlock(CloseCode.WRONG_CREDENTIALS)
            return
        }

        val isCorrect = verifyPassword(account.password, packet.password)
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
            talker.closeWithBlock(CloseCode.NODE_ALREADY_EXISTS, "Node at this address already exists")
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

    val encryptingDispatcher = Dispatchers.IO

    private suspend fun verifyPassword(real: EncryptedPassword, submitted: String): Boolean {
        return withContext(encryptingDispatcher) {
            real.isEqual(submitted)
        }
    }
}