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
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.StorageTable
import dev.nikdekur.nexushub.storage.account.AccountDAO
import dev.nikdekur.nexushub.storage.index.indexOptions
import dev.nikdekur.nexushub.storage.mongo.getTable
import dev.nikdekur.nexushub.storage.request.eq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class StorageAccountsService(
    override val app: NexusHubServer
) : AccountsService {

    val protectionService: ProtectionService by inject()
    val storage: StorageService by inject()

    lateinit var table: StorageTable<AccountDAO>

    override fun getAccounts(): Collection<Account> {
        return accounts.values
    }

    val accounts = ConcurrentHashMap<String, Account>()


    override fun onEnable(): Unit = runBlocking {
        table = storage.getTable("accounts")
        table.createIndex("login", mapOf("login" to 1), indexOptions {
            unique = true
        })

        table.find().toList().forEach(::registerAccount)
    }

    override fun onDisable() {
        accounts.clear()
    }


    override fun getAccount(login: String): Account? {
        return accounts[login]
    }

    fun registerAccount(dao: AccountDAO): Account {
        val password = protectionService.deserializePassword(dao.password)
        return Account(app, dao, password, dao.allowedScopes.toMutableSet()).also {
            accounts[dao.login] = it
        }
    }

    override suspend fun updateAccount(dao: AccountDAO) {
        table.replaceOne(dao, AccountDAO::login eq dao.login)
    }

    suspend fun createAccount(dao: AccountDAO): Account {
        table.insertOne(dao)
        return registerAccount(dao)
    }

    override suspend fun createAccount(
        login: String,
        password: String,
        allowedScopes: Set<String>
    ): Account {
        require(getAccount(login) == null) { "Account with login \"$login\" already exists" }
        val password = protectionService.createPassword(password)
        val dao = AccountDAO(
            login = login,
            password = password.serialize(),
            allowedScopes = allowedScopes
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


    val encryptingDispatcher = Dispatchers.Default

    override suspend fun matchPassword(real: Password, test: String): Boolean {
        return withContext(encryptingDispatcher) {
            real.isEqual(test)
        }
    }
}