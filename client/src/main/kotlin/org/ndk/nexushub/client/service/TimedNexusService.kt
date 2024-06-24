package org.ndk.nexushub.client.service

import dev.nikdekur.ndkore.scheduler.Scheduler
import dev.nikdekur.ndkore.scheduler.SchedulerTask
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.sesion.Session
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

abstract class TimedNexusService<H, S>(
    val scheduler: Scheduler,
    hub: NexusHub,
    val sessionLiveTime: Duration
) : AbstractNexusService<H, S>(hub) {

    val sessionLiveTimeMs = sessionLiveTime.toMillis()
    val tasks = ConcurrentHashMap<String, SchedulerTask>()

    override fun getExistingSession(holderId: String): Session<H, S>? {
        val session = super.getExistingSession(holderId)
        if (session != null) {
            val task = scheduler.runTaskLater(sessionLiveTimeMs, session::stop)
            tasks.put(session.id, task)?.cancel()
        }
        return session
    }

    override suspend fun startSession(holder: H): Session<H, S> {
        val session = super.startSession(holder)
        val task = scheduler.runTaskLater(sessionLiveTimeMs, session::stop)
        tasks.put(session.id, task)?.cancel()
        return session
    }

    override suspend fun stopSession(holderId: String): Session<H, S>? {
        val session = super.stopSession(holderId)
        tasks.remove(holderId)?.cancel()
        return session
    }

    override fun removeSession(session: Session<H, S>) {
        super.removeSession(session)
        tasks.remove(session.id)?.cancel()
    }
}
