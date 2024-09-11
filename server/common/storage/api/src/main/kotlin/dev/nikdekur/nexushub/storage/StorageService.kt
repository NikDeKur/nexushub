/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface StorageService {

    val scope: CoroutineScope

    fun getAllTables(): Flow<String>
    fun <T : Any> getTable(name: String, clazz: Class<T>): StorageTable<T>
}

inline fun <reified T : Any> StorageService.getTable(name: String): StorageTable<T> {
    return getTable(name, T::class.java)
}