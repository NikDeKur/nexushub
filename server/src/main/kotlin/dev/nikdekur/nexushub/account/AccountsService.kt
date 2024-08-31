/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.account

import dev.nikdekur.nexushub.protection.Password
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.storage.account.AccountDAO

interface AccountsService : NexusHubService {

    suspend fun getAccounts(): Collection<Account>

    suspend fun getAccount(login: String): Account?
    suspend fun updateAccount(dao: AccountDAO)
    suspend fun createAccount(login: String, password: String, allowedScopes: Iterable<String>): Account

    /**
     * Deletes an account from the database and the cache.
     *
     * Note that method will not disconnect clients that are currently using the account.
     *
     * @param login The login of the account to delete.
     */
    suspend fun deleteAccount(login: String)


    suspend fun matchPassword(pass1: Password, pass2: String): Boolean

}