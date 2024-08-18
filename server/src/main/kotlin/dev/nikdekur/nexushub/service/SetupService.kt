/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.service

import dev.nikdekur.ndkore.ext.info
import dev.nikdekur.ndkore.ext.input
import dev.nikdekur.ndkore.ext.trace
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.auth.account.AccountsService
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class SetupService(
    override val app: NexusHubServer
) : NexusHubService {

    val logger = LoggerFactory.getLogger(javaClass)

    val accountsService: AccountsService by inject()

    override fun onLoad() {
        runBlocking {
            rootCreationProtocol(accountsService)
        }
    }


    suspend fun rootCreationProtocol(accountService: AccountsService) {
        val rootAccount = accountService.getAccount("root")
        if (rootAccount != null) {
            logger.trace { "Root account already exists" }
            return
        }
        logger.trace { "Root account does not exist" }

        val rootPassword = askRootPassword()
        accountService.createAccount("root", rootPassword, setOf())

        logger.info { "Root account created" }
    }

    fun askRootPassword(): String {
        println("Hello! It seems that the root account is not created yet.")
        println("Let's create it now. Root account is the most powerful account in the system.")
        println("It used to manage other accounts, not for projects. Root account can't have scopes.")
        println("When choosing your root password, be sure to choose a strong password.")
        return input("Enter root password: ")
    }
}