package org.ndk.nexushub.client.hook

import java.util.concurrent.atomic.AtomicInteger

class WhenSessionLoadedHook(val customId: String? = null, val code: () -> Unit) : Hook() {

    override val id: String = customId ?: "WhenLoadedHook-${hookId.incrementAndGet()}"

    override fun invoke() {
        return code()
    }

    companion object {
        val hookId = AtomicInteger(0)
    }
}