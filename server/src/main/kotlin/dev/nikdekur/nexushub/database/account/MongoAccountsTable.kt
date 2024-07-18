/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.database.account

import com.mongodb.client.result.DeleteResult
import com.mongodb.kotlin.client.coroutine.MongoCollection
import dev.nikdekur.nexushub.database.mongo.eq
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList

class MongoAccountsTable(
    val table: MongoCollection<AccountDAO>
) : AccountsTable {

    override suspend fun fetchAllAccounts(): List<AccountDAO> {
        return table.find().toList()
    }

    override suspend fun newAccount(dao: AccountDAO) {
        table.insertOne(dao)
    }

    override suspend fun updateAccount(dao: AccountDAO) {
        val filter = "login" eq dao.login
        table.replaceOne(filter, dao)
    }

    override suspend fun findAccount(login: String): AccountDAO? {
        val filter = "login" eq login
        return table
            .find(filter)
            .singleOrNull()
    }

    override suspend fun deleteAccount(login: String): DeleteResult {
        val filter = "login" eq login
        return table.deleteOne(filter)
    }
}

