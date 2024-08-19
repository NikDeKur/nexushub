/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.storage.mongo

import com.mongodb.client.model.Filters
import com.mongodb.client.model.IndexOptions
import com.mongodb.kotlin.client.coroutine.MongoCollection
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import dev.nikdekur.nexushub.storage.request.CompOperator
import dev.nikdekur.nexushub.storage.request.Filter
import dev.nikdekur.nexushub.storage.request.Order
import dev.nikdekur.nexushub.storage.request.Sort
import kotlinx.coroutines.flow.firstOrNull
import org.bson.BsonDocument
import org.bson.Document
import org.bson.conversions.Bson

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

inline fun mongoIndexOptions(block: IndexOptions.() -> Unit): IndexOptions {
    return IndexOptions().apply(block)
}

fun Filter.toBson(): Bson {
    return when (operator) {
        CompOperator.EQUALS -> Filters.eq(key, value)
        CompOperator.NOT_EQUALS -> Filters.ne(key, value)
        CompOperator.GREATER_THAN -> Filters.gt(key, value)
        CompOperator.LESS_THAN -> Filters.lt(key, value)
        CompOperator.GREATER_THAN_OR_EQUALS -> Filters.gte(key, value)
        CompOperator.LESS_THAN_OR_EQUALS -> Filters.lte(key, value)
    }
}

inline fun Array<out Filter>.toBson(): Bson {
    if (isEmpty()) return BsonDocument()
    return Filters.and(map(Filter::toBson))
}

inline fun Iterable<Filter>.toBson(): Bson {
    if (none()) return BsonDocument()
    return Filters.and(map(Filter::toBson))
}

inline fun Sort.toBson(): Bson {
    val ascending = order == Order.ASCENDING
    val num = if (ascending) 1 else -1
    return Document(field, num)
}
