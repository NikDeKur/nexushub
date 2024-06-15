package org.ndk.nexushub.client.hook

import dev.nikdekur.ndkore.interfaces.Snowflake


abstract class Hook : Snowflake<String>, () -> Unit {
    override fun toString(): String {
        return "${javaClass.simpleName}(id=$id)"
    }
}