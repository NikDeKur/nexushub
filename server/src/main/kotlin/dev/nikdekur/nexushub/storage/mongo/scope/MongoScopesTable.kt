/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.storage.mongo.scope

import com.mongodb.kotlin.client.coroutine.MongoCollection
import dev.nikdekur.nexushub.storage.mongo.eq
import dev.nikdekur.nexushub.storage.scope.ScopeDAO
import dev.nikdekur.nexushub.storage.scope.ScopesTable
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson


/**
 * Class responsible for table contains all scopes DAOs
 */
class MongoScopesTable(
    val collection: MongoCollection<ScopeDAO>
) : ScopesTable {


    override suspend fun createScope(dao: ScopeDAO) {
        collection.insertOne(dao)
    }

    override suspend fun findScope(scope: String): ScopeDAO? {
        return collection
            .find(filterScope(scope))
            .firstOrNull()
    }

    override suspend fun updateScope(scopeDAO: ScopeDAO) {
        collection
            .replaceOne(filterScope(scopeDAO.name), scopeDAO)
    }

    override suspend fun deleteScope(scope: String) {
        collection
            .deleteOne(filterScope(scope))
    }


    inline fun filterScope(scope: String): Bson = ScopeDAO::name.name eq scope

}