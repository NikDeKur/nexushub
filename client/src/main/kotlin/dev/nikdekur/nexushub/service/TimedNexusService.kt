/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.service

import dev.nikdekur.ndkore.scheduler.Scheduler
import dev.nikdekur.ndkore.scheduler.SchedulerTask
import dev.nikdekur.nexushub.NexusHub
import dev.nikdekur.nexushub.sesion.Session
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

abstract class TimedNexusService<H, S>(
    val scheduler: Scheduler,
    hub: NexusHub,
    val sessionLiveTime: Duration
) : AbstractNexusService<H, S>(hub) {

    val sessionLiveTimeMs = sessionLiveTime.inWholeMilliseconds
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

    override suspend fun stopSession(session: Session<H, S>) {
        super.stopSession(session)
        tasks.remove(session.id)?.cancel()
    }

    override fun removeSession(session: Session<H, S>) {
        super.removeSession(session)
        tasks.remove(session.id)?.cancel()
    }
}
