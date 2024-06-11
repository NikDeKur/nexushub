@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.database.account

import com.mongodb.client.model.Filters
import com.mongodb.client.result.DeleteResult
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.singleOrNull
import org.bson.conversions.Bson
import org.ndk.nexushub.database.Database

object AccountsTable {

    const val TABLE_NAME = "accounts"
    lateinit var table: MongoCollection<AccountDAO>

    fun init() {
        val db = Database.database
        table = db.getCollection(TABLE_NAME)
    }

    suspend fun newAccount(dao: AccountDAO) {
        table.insertOne(dao)
    }

    suspend fun updateAccount(dao: AccountDAO) {
        val filter = "login" eq dao.login
        table.replaceOne(filter, dao)
    }

    suspend fun fetchAccount(login: String): AccountDAO? {
        val filter = "login" eq login
        return table
            .find(filter)
            .singleOrNull()
    }

    suspend fun deleteAccount(login: String): DeleteResult {
        val filter = "login" eq login
        return table.deleteOne(filter)
    }
}


inline infix fun String.eq(value: Any): Bson = Filters.eq(this, value)