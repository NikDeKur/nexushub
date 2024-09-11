@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.access.auth


data class RootToken(
    val token: String,
    val validBy: Long
) {

    fun isValid(): Boolean {
        return System.currentTimeMillis() < validBy
    }

    inline fun toMap(): Map<String, Any> {
        return mapOf(
            "token" to token,
            "valid_by" to validBy
        )
    }
}