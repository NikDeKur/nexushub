/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth

import dev.nikdekur.ndkore.service.get
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.account.StorageAccountsService
import dev.nikdekur.nexushub.lightWeightNexusHubServer
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.node.RuntimeNodesService
import dev.nikdekur.nexushub.protection.password.ProtectionService
import dev.nikdekur.nexushub.protection.password.none.NoneProtectionService
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.runtime.RuntimeStorageService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach

class AccountAuthenticationServiceTest : AuthenticationServiceTest {

    @BeforeEach
    fun setup(): Unit = runBlocking {
        val server = lightWeightNexusHubServer {
            service(::RuntimeStorageService, StorageService::class)
            service(::NoneProtectionService, ProtectionService::class)
            service(::StorageAccountsService, AccountsService::class)
            service(::RuntimeNodesService, NodesService::class)
            service(::AccountAuthenticationService, AuthenticationService::class)
        }
        service = server.get()

        val accountsService: AccountsService = server.get()
        accountsService.createAccount("login1", "password1", setOf("scope1"))
        accountsService.createAccount("login2", "password2", setOf("scope2"))
    }

    override lateinit var service: AuthenticationService
}