/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.setup

import dev.nikdekur.ndkore.ext.info
import dev.nikdekur.ndkore.ext.trace
import dev.nikdekur.ndkore.service.dependencies
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.account.AccountsService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class EnvironmentSetupService(
    override val app: NexusHubServer
) : SetupService {

    override val dependencies = dependencies {
        after(AccountsService::class)
    }

    val logger = LoggerFactory.getLogger(javaClass)

    val accountsService: AccountsService by inject()

    override fun onEnable() {
        runBlocking {
            rootCreationProtocol()
        }
    }


    suspend fun rootCreationProtocol() {
        val rootAccount = accountsService.getAccount("root")
        if (rootAccount != null) {
            logger.trace { "Root account already exists" }
            return
        }
        logger.trace { "Root account does not exist" }

        val rootPassword = askRootPassword()
        accountsService.createAccount("root", rootPassword, setOf())

        logger.info { "Root account created" }
    }

    fun askRootPassword(): String {
        val description = listOf(
            "Hello! It seems that the root account is not created yet.",
            "Let's create it now. Root account is the most powerful account in the system.",
            "It used to manage other accounts, not for projects. Root account can't have scopes.",
            "When choosing your root password, be sure to choose a strong password.",
            "Enter the root password: "
        )
        return app.environment.requestValue("root_password", description.joinToString("\n"))
            ?: throw IllegalStateException("Root password is required to continue")
    }
}