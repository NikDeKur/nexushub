package org.ndk.nexushub.client.service

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import org.ndk.klib.info
import org.ndk.klib.parallel
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.sesion.Session
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractNexusService<H : Any, S : Session<H, S>>(
    override val hub: NexusHub
) : NexusService<H, S> {

    override val logger: Logger by lazy {
        LoggerFactory.getLogger("NexusHub-$scope")
    }

    open val sessionsLimit: Long = -1
    open val saveParallelism: Int = 32

    val sessionsCache: Cache<String, S> by lazy {
        preBuildCache().build()
    }

    open fun preBuildCache(): CacheBuilder<String, S> {
        return CacheBuilder.newBuilder()
            .concurrencyLevel(4)
            .apply {
                if (sessionsLimit > 0) {
                    maximumSize(sessionsLimit)
                }
            }
            .removalListener {
                if (!isRunning) return@removalListener
                hub.blockingScope.launch {
                    it.value!!.stop()
                }
            }
    }

    override val sessions: Collection<S>
        get() = sessionsCache.asMap().values

    override var isRunning: Boolean = false

    override fun start() {
        isRunning = true
    }

    override suspend fun stop() {
        logger.info { "Stopping service $scope" }
        isRunning = false
        val sessions = sessionsCache.asMap().values
        hub.blockingScope.parallel(saveParallelism, sessions) {
            it.stop()
        }.awaitAll()

        sessionsCache.invalidateAll()
    }

    override fun getExistingSession(holderId: String): S? {
        return sessionsCache.getIfPresent(holderId)
    }


    override suspend fun stopSession(holderId: String): S? {
        val session = getExistingSession(holderId)
        if (session != null) {
            // Removal Listener will execute stopSessionInternal
            sessionsCache.invalidate(holderId)
        }
        return session
    }

    override suspend fun startSession(holder: H): S {
        stopSession(getId(holder))
        val session = createSession(holder)
        sessionsCache.put(getId(holder), session)
        session.loadData()
        return session
    }
}