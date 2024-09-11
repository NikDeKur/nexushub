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
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.node.isNodeExists
import dev.nikdekur.nexushub.service.NexusHubService

class AccountAuthenticationService(
    override val app: NexusHubServer
) : NexusHubService(), AuthenticationService {

    val nodesService: NodesService by inject()
    val accountsService: AccountsService by inject()

    override suspend fun authenticate(talker: Talker, credentials: Credentials): AuthenticationService.AuthResult {
        val account = accountsService.getAccount(credentials.login)
        if (account == null)
            return AuthenticationService.AuthResult.AccountNotFound


        val isCorrect = account.password.isEqual(credentials.password)
        if (!isCorrect)
            return AuthenticationService.AuthResult.WrongCredentials


        val nodeStr = credentials.node
        if (!isValidNodeName(nodeStr))
            return AuthenticationService.AuthResult.NodeNameInvalid


        if (nodesService.isNodeExists(talker))
            return AuthenticationService.AuthResult.NodeAtAddressAlreadyExists


        if (nodesService.isNodeExists(nodeStr))
            return AuthenticationService.AuthResult.NodeAlreadyExists


        val node = nodesService.newNode(talker, account, nodeStr)
        return AuthenticationService.AuthResult.Success(node)
    }

    val validNodePattern = Regex("[a-zA-Z0-9_-]{4,32}")
    fun isValidNodeName(node: String): Boolean {
        return validNodePattern.matches(node)
    }
}
