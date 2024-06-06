package org.ndk.nexushub.database.scope

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