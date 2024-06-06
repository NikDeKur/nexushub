package org.ndk.nexushub.client.service

import com.google.common.cache.CacheBuilder
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.sesion.Session
import java.time.Duration

abstract class TimedNexusService<H : Any, S : Session<H, S>>(
    hub: NexusHub,
    val sessionLiveTime: Duration
) : AbstractNexusService<H, S>(hub) {

    override fun preBuildCache(): CacheBuilder<String, S> {
        return super.preBuildCache()
            .expireAfterWrite(sessionLiveTime)
            .expireAfterAccess(sessionLiveTime)
    }
}
