/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage.account

interface AccountsTable {
    suspend fun fetchAllAccounts(): List<AccountDAO>
    suspend fun findAccount(login: String): AccountDAO?
    suspend fun newAccount(dao: AccountDAO)
    suspend fun updateAccount(dao: AccountDAO)
    suspend fun deleteAccount(login: String)
}