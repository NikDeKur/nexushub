/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth.account

import dev.nikdekur.nexushub.auth.password.PasswordEncryptor
import dev.nikdekur.nexushub.database.account.AccountDAO
import dev.nikdekur.nexushub.database.account.AccountsTable
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class AccountsServiceImpl(
    val table: AccountsTable
) : AccountsService {



    val accounts = ConcurrentHashMap<String, Account>()

    init {
        runBlocking {
            table.fetchAllAccounts().forEach {
                val account = it.toHighLevel()
                accounts[account.login] = account
            }
            table
        }
    }




    override fun getAccount(login: String): Account? {
        return accounts[login]
    }

    override suspend fun updateAccount(dao: AccountDAO) {
        table.updateAccount(dao)
    }

    override suspend fun newAccount(dao: AccountDAO): Account {
        table.newAccount(dao)
        val account = dao.toHighLevel()
        accounts[account.login] = account
        return account
    }

    override suspend fun newAccount(login: String, password: String, allowedScopes : Set<String>): Account {
        val encryptedPassword = PasswordEncryptor.encryptNew(password)
        val dao = AccountDAO(
            login = login,
            password = encryptedPassword.hex,
            salt = encryptedPassword.salt.hex,
            allowedScopes = allowedScopes
        )
        return newAccount(dao)
    }


    /**
     * Deletes an account from the database and the cache.
     *
     * Note that method will not disconnect clients that are currently using the account.
     *
     * @param login The login of the account to delete.
     */
    override suspend fun deleteAccount(login: String) {
        table.deleteAccount(login)
        accounts.remove(login)
    }
}