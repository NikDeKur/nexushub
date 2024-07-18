/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth.account

import dev.nikdekur.nexushub.database.account.AccountDAO

interface AccountsService {

    fun getAccount(login: String): Account?
    suspend fun updateAccount(dao: AccountDAO)
    suspend fun newAccount(dao: AccountDAO): Account
    suspend fun newAccount(login: String, password: String, allowedScopes : Set<String>): Account

    /**
     * Deletes an account from the database and the cache.
     *
     * Note that method will not disconnect clients that are currently using the account.
     *
     * @param login The login of the account to delete.
     */
    suspend fun deleteAccount(login: String)

}