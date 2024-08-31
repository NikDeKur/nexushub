/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.access.config

import dev.nikdekur.nexushub.account.Account
import dev.nikdekur.nexushub.service.NexusHubService

/**
 * # Configuration Access Service
 *
 * Service responsible for accessing and managing the configuration of the server.
 */
interface ConfigurationAccessService : NexusHubService {


    suspend fun listAccounts(): Collection<Account>

    suspend fun createAccount(login: String, password: String, scopes: Iterable<String>): AccountCreationResult
    sealed interface AccountCreationResult {
        data class Success(val account: Account) : AccountCreationResult
        object AccountAlreadyExists : AccountCreationResult
    }

    suspend fun deleteAccount(login: String): AccountDeletionResult
    sealed interface AccountDeletionResult {
        object Success : AccountDeletionResult
        object AccountNotFound : AccountDeletionResult
    }

    suspend fun changePassword(login: String, newPassword: String): PasswordChangeResult
    sealed interface PasswordChangeResult {
        object Success : PasswordChangeResult
        object AccountNotFound : PasswordChangeResult
    }


    suspend fun listAccountScopes(login: String): AccountScopesListResult
    sealed interface AccountScopesListResult {
        data class Success(val scopes: Collection<String>) : AccountScopesListResult
        object AccountNotFound : AccountScopesListResult
    }

    suspend fun changeAccountScopes(login: String, action: Action, scopes: Iterable<String>): AccountScopesChangeResult
    sealed interface AccountScopesChangeResult {
        object Success : AccountScopesChangeResult
        object AccountNotFound : AccountScopesChangeResult
    }

    enum class Action {
        ADD,
        REMOVE,
        SET,
        CLEAR // Clear all scopes, scopes parameter is ignored
    }

}