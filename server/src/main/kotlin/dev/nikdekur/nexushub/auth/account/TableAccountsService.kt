/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth.account

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.protection.Password
import dev.nikdekur.nexushub.protection.ProtectionService
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.storage.account.AccountDAO
import dev.nikdekur.nexushub.storage.account.AccountsTable
import dev.nikdekur.nexushub.storage.mongo.MongoAccountsTable
import dev.nikdekur.nexushub.storage.mongo.MongoStorageService
import dev.nikdekur.nexushub.storage.mongo.ensureCollectionExists
import dev.nikdekur.nexushub.storage.mongo.indexOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bson.Document
import java.util.concurrent.ConcurrentHashMap

class TableAccountsService(
    override val app: NexusHubServer,
    val database: MongoStorageService
) : NexusHubService, AccountsService {

    val protectionService: ProtectionService by inject()

    lateinit var table: AccountsTable

    override fun getAccounts(): Collection<Account> {
        return accounts.values
    }

    val accounts = ConcurrentHashMap<String, Account>()


    override fun onLoad(): Unit = runBlocking {
        table = MongoAccountsTable(
            database.database.ensureCollectionExists<AccountDAO>("accounts") {
                val indexOptions = indexOptions {
                    unique(true)
                }

                createIndex(Document("login", 1), indexOptions)
            }
        )

        table.fetchAllAccounts().forEach(::registerAccount)
    }

    override fun onUnload() {
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
        table.updateAccount(dao)
    }

    suspend fun createAccount(dao: AccountDAO): Account {
        table.newAccount(dao)
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
        table.deleteAccount(login)
        accounts.remove(login)
    }


    val encryptingDispatcher = Dispatchers.Default

    override suspend fun matchPassword(real: Password, test: String): Boolean {
        return withContext(encryptingDispatcher) {
            real.isEqual(test)
        }
    }
}