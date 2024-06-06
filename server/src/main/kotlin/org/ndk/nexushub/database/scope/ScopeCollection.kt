@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.database.scope

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import org.ndk.nexushub.network.NexusData
import org.ndk.nexushub.util.indexOptions

class ScopeCollection(val table: MongoCollection<Document>) {

    suspend fun loadOrNull(id: String): NexusData? {
        val filter = idFilter(id)
        return table.find(filter)
            .singleOrNull()
            ?.let { HashMap(it)
                .apply {
                    remove("_id")
                    remove("holderId")
                }}
    }


    suspend fun save(id: String, new: NexusData) {
        val old = loadOrNull(id)

        // If the existing data are same as the new data, return
        if (old == new) return

        // If the new data is empty and old data exists, delete the data
        if (old != null && new.isEmpty()) {
            table.deleteOne(idFilter(id))
            return
        }

        val updateFields = new.entries.map { (key, value) -> set(key, value) }

        // If the old data does not exist, insert the new data
        if (old == null) {
            table.insertOne(Document("holderId", id).apply {
                new.forEach { (key, value) -> append(key, value) }
            })
        } else {
            // If the old data exists, update the data
            table.updateOne(idFilter(id), combine(updateFields))
        }
    }



    suspend fun createIndex(field: String, unique: Boolean) {
        table.createIndex(Document(field, 1), indexOptions {
            unique(unique)
        })
    }


    inline fun idFilter(id: String): Bson {
        return Filters.eq("holderId", id)
    }

    suspend fun getLeaderboard(field: String, limit: Int): List<Document> {
        return table.find()
            .sort(Document(field, -1))
            .limit(limit)
            .toList()
    }

    suspend fun getTopPosition(holderId: String, field: String, value: Double): Long {
        // Count the number of documents that have a value greater than the given value,
        // And the holderId is not the given holderId (to exclude the given holderId)
        val filter = Filters.and(
            Filters.gt(field, value),
            Filters.ne("holderId", holderId)
        )
        return table.countDocuments(filter) + 1L
    }
}