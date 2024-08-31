/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.auth.AuthenticationService.AuthResult
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.node.isNodeExists
import dev.nikdekur.nexushub.packet.PacketAuth
import org.slf4j.LoggerFactory

class AccountAuthenticationService(
    override val app: NexusHubServer
) : AuthenticationService {

    val logger = LoggerFactory.getLogger(javaClass)

    val nodesService: NodesService by inject()
    val accountsService: AccountsService by inject()

    override suspend fun authenticate(talker: Talker, packet: PacketAuth): AuthResult {
        val account = accountsService.getAccount(packet.login)
        if (account == null)
            return AuthResult.AccountNotFound


        val isCorrect = accountsService.matchPassword(account.password, packet.password)
        if (!isCorrect)
            return AuthResult.WrongCredentials


        val nodeStr = packet.node
        if (!isValidNodeName(nodeStr))
            return AuthResult.NodeNameInvalid


        if (nodesService.isNodeExists(talker))
            return AuthResult.NodeAtAddressAlreadyExists


        if (nodesService.isNodeExists(nodeStr))
            return AuthResult.NodeAlreadyExists


        val node = nodesService.newNode(talker, account, nodeStr)
        return AuthResult.Success(node)
    }

    val validNodePattern = Regex("[a-zA-Z0-9_-]{4,32}")
    fun isValidNodeName(node: String): Boolean {
        return validNodePattern.matches(node)
    }
}
