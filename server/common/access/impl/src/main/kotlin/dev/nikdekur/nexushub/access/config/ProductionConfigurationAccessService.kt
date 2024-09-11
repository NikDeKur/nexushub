/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.access.config

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.AccountCreationResult
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.AccountDeletionResult
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.AccountScopesListResult
import dev.nikdekur.nexushub.access.config.ConfigurationAccessService.PasswordChangeResult
import dev.nikdekur.nexushub.account.Account
import dev.nikdekur.nexushub.account.AccountsService
import dev.nikdekur.nexushub.protection.password.ProtectionService
import dev.nikdekur.nexushub.service.NexusHubService

class ProductionConfigurationAccessService(
    override val app: dev.nikdekur.nexushub.NexusHubServer
) : NexusHubService(), ConfigurationAccessService {

    val protectionService: ProtectionService by inject()
    val accountsService: AccountsService by inject()

    override suspend fun reload() {
        app.servicesManager.reload()
    }

    override suspend fun listAccounts(): Collection<Account> {
        return accountsService.getAccounts()
    }

    override suspend fun createAccount(
        login: String,
        password: String,
        scopes: Iterable<String>
    ): AccountCreationResult {
        val existingAccount = accountsService.getAccount(login)
        if (existingAccount != null)
            return AccountCreationResult.AccountAlreadyExists

        val account = accountsService.createAccount(login, password, scopes)
        return AccountCreationResult.Success(account)
    }

    override suspend fun deleteAccount(login: String): AccountDeletionResult {
        val account = accountsService.getAccount(login)
        if (account == null)
            return AccountDeletionResult.AccountNotFound

        accountsService.deleteAccount(account.login)
        return AccountDeletionResult.Success
    }

    override suspend fun changePassword(
        login: String,
        newPassword: String
    ): PasswordChangeResult {
        val account = accountsService.getAccount(login)
        if (account == null)
            return PasswordChangeResult.AccountNotFound

        val newPasswordEncrypted = protectionService.createPassword(newPassword)
        account.changePassword(newPasswordEncrypted)

        return PasswordChangeResult.Success
    }

    override suspend fun listAccountScopes(login: String): AccountScopesListResult {
        val account = accountsService.getAccount(login)
        if (account == null)
            return AccountScopesListResult.AccountNotFound

        return AccountScopesListResult.Success(account.getScopes())
    }

    override suspend fun changeAccountScopes(
        login: String,
        action: ConfigurationAccessService.Action,
        scopes: Iterable<String>
    ): ConfigurationAccessService.AccountScopesChangeResult {
        TODO("Not yet implemented")
    }


}