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
import kotlinx.coroutines.flow.firstOrNull
import org.bson.conversions.Bson
import kotlin.reflect.KProperty

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


inline infix fun String.eq(value: Any): Bson = Filters.eq(this, value)
inline infix fun <T> KProperty<T>.eq(value: Any): Bson = Filters.eq(this.name, value)