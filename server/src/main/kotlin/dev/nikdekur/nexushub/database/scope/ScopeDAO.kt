/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.database.scope

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class ScopeDAO(
    @BsonId
    val id: ObjectId = ObjectId(),
    val name: String,
    val indexes: Set<String>
) {

    companion object {
        fun new(name: String, indexes: Set<String>): ScopeDAO {
            return ScopeDAO(name = name, indexes = indexes)
        }
    }
}