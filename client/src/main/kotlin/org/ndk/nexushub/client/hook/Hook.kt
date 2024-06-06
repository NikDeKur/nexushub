package org.ndk.nexushub.client.hook

import org.ndk.global.interfaces.Snowflake

abstract class Hook : Snowflake<String>, () -> Unit {
    override fun toString(): String {
        return "${javaClass.simpleName}(id=$id)"
    }
}