package org.ndk.nexushub.client.hook

import dev.nikdekur.ndkore.ext.*
import org.ndk.nexushub.client.sesion.Session

class HooksExecutor(val session: Session<*, *>) {
    val hooks = ArrayList<Hook>()

    fun addHook(hook: Hook) {
        hooks.add(hook)
    }

    fun executeHooks() {
        hooks.mutableForEach {
            try {
                it()
            } catch (e: Exception) {
                session.service.logger.warn { "Failed to execute hook $it" }
                e.printStackTrace()
            }
        }
    }
}