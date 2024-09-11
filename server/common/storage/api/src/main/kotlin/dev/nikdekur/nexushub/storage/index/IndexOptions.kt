/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage.index

data class IndexOptions(
    val name: String,
    val unique: Boolean = false
)

class IndexOptionsBuilder {
    var name: String = ""
    var unique: Boolean = false

    fun build(): IndexOptions {
        return IndexOptions(name, unique)
    }
}

inline fun indexOptions(block: IndexOptionsBuilder.() -> Unit): IndexOptions {
    return IndexOptionsBuilder().apply(block).build()
}