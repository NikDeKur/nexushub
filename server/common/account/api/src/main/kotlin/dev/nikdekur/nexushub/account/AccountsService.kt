/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.account

interface AccountsService {

    /**
     * Gets all accounts.
     *
     * @return All accounts.
     */
    suspend fun getAccounts(): Collection<Account>

    /**
     * Gets an account by its login.
     *
     * @param login The login of the account to get.
     * @return The account with the given login, or null if no such account exists.
     */
    suspend fun getAccount(login: String): Account?

    /**
     * Creates a new account with the given login, password and allowed scopes.
     *
     * @param login The login of the account to create.
     * @param password The password of the account to create.
     * @param allowedScopes The scopes that the account is allowed to access.
     * @return The created account.
     * @throws AccountAlreadyExistsException If an account with the given login already exists.
     */
    suspend fun createAccount(login: String, password: String, allowedScopes: Iterable<String>): Account

    /**
     * Deletes an account from the database and the cache.
     *
     * Note that method will not disconnect clients that are currently using the account.
     *
     * @param login The login of the account to delete.
     */
    suspend fun deleteAccount(login: String)
}