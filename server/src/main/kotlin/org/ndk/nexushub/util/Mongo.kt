package org.ndk.nexushub.util

import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.firstOrNull

suspend inline fun <reified T : Any> MongoDatabase.ensureCollectionExists(
    name: String,
    create: MongoCollection<T>.() -> Unit = {}
): MongoCollection<T> {

    val existingTable = listCollectionNames().firstOrNull {
        it == name
    }

    if (existingTable != null) {
        return getCollection(name)
    }


    createCollection(name)
    val collection = getCollection<T>(name)
    create(collection)
    return collection
}

inline fun indexOptions(block: IndexOptions.() -> Unit): IndexOptions {
    return IndexOptions().apply(block)
}