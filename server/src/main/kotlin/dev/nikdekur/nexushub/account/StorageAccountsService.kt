/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.account

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.protection.Password
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.StorageTable
import dev.nikdekur.nexushub.storage.account.AccountDAO
import dev.nikdekur.nexushub.storage.getTable
import dev.nikdekur.nexushub.storage.index.indexOptions
import dev.nikdekur.nexushub.storage.request.eq
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class StorageAccountsService(
    override val app: NexusHubServer
) : NexusHubService(), AccountsService {

    val protectionService: ProtectionService by inject()
    val storage: StorageService by inject()

    lateinit var table: StorageTable<AccountDAO>

    val accounts = ConcurrentHashMap<String, Account>()

    override fun onEnable(): Unit = runBlocking {
        table = storage.getTable("accounts")
        table.createIndex("login", mapOf("login" to 1), indexOptions {
            unique = true
        })

        table.find().toList().forEach {
            registerAccount(it)
        }
    }


    override fun onDisable() {
        accounts.clear()
    }

    override suspend fun getAccounts(): Collection<Account> {
        return accounts.values
    }


    override suspend fun getAccount(login: String): Account? {
        return accounts[login]
    }

    suspend fun registerAccount(dao: AccountDAO): Account {
        val password = protectionService.deserializePassword(dao.password)
        val account = BasicAccount(dao.login, password)
        return AccountWrapper(this, account).also {
            dao.allowedScopes.forEach {
                account.allowScope(it)
            }
            accounts[dao.login] = account
        }
    }

    suspend fun update(dao: Account) {
        val dao = AccountDAO(
            login = dao.login,
            password = dao.password.serialize(),
            allowedScopes = dao.getScopes()
        )
        table.replaceOne(dao, AccountDAO::login eq dao.login)
    }

    suspend fun createAccount(dao: AccountDAO): Account {
        table.insertOne(dao)
        return registerAccount(dao)
    }

    override suspend fun createAccount(
        login: String,
        password: String,
        allowedScopes: Iterable<String>
    ): Account {
        if (getAccount(login) != null)
            throw AccountAlreadyExistsException(login)

        val password = protectionService.createPassword(password)
        val dao = AccountDAO(
            login = login,
            password = password.serialize(),
            allowedScopes = allowedScopes.toSet()
        )
        return createAccount(dao)
    }


    /**
     * Deletes an account from the database and the cache.
     *
     * Note that method will not disconnect clients that are currently using the account.
     *
     * @param login The login of the account to delete.
     */
    override suspend fun deleteAccount(login: String) {
        val filter = "login" eq login
        table.deleteOne(filter)
        accounts.remove(login)
    }


    class AccountWrapper(
        val service: StorageAccountsService,
        val account: Account
    ) : Account by account {

        override suspend fun changePassword(newPassword: Password) {
            account.changePassword(newPassword)
            service.update(this)
        }

        override suspend fun allowScope(scope: String) {
            account.allowScope(scope)
            service.update(this)
        }

        override suspend fun removeScope(scope: String) {
            account.removeScope(scope)
            service.update(this)
        }

        override suspend fun clearScopes() {
            account.clearScopes()
            service.update(this)
        }
    }
}