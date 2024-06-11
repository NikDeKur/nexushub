@file:Suppress("NOTHING_TO_INLINE")

package org.ndk.nexushub.database.scope

import com.mongodb.client.model.Filters
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import org.ndk.nexushub.network.NexusData
import org.ndk.nexushub.util.indexOptions

class ScopeCollection(val collection: MongoCollection<Document>) {

    suspend fun loadOrNull(id: String): NexusData? {
        val filter = idFilter(id)
        return collection.find(filter)
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
            collection.deleteOne(idFilter(id))
            return
        }

        val data = Document("holderId", id).apply {
            new.forEach { (key, value) -> append(key, value) }
        }

        // If the old data does not exist, insert the new data
        if (old == null) {
            collection.insertOne(data)
        } else {
            // If the old data exists, update the data
            collection.replaceOne(idFilter(id), data)
        }
    }



    suspend fun createIndex(field: String, unique: Boolean) {
        collection.createIndex(Document(field, 1), indexOptions {
            unique(unique)
        })
    }


    inline fun idFilter(id: String): Bson {
        return Filters.eq("holderId", id)
    }

    suspend fun getLeaderboard(field: String, startFrom: Int, limit: Int): List<Document> {
        return collection
            .find()
            .sort(Document(field, -1))
            .limit(limit)
            .skip(startFrom)
            .toList()
    }

    /**
     * Get the top position in the leaderboard for the given field of the given holder
     *
     * If no field is found for the holder or field is not a double, null is returned
     *
     * The top position is started from 0, so the top position is 0, the second position is 1, and so on
     *
     * @param holderId holder id
     * @param field field to get the top position for
     * @param value value to compare
     * @return the top position in the leaderboard for the given field of the given holder
     */
    suspend fun getTopPosition(holderId: String, field: String, value: Double): Long {
        // Count the number of documents that have a value greater than the given value,
        // And the holderId is not the given holderId (to exclude the given holderId)
        val filter = Filters.and(
            Filters.gt(field, value),
            Filters.ne("holderId", holderId)
        )
        val count = collection.countDocuments(filter)
        return count
    }
}