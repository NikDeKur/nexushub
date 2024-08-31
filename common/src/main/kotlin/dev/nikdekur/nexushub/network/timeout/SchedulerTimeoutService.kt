/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.timeout

import dev.nikdekur.ndkore.map.MutableListsMap
import dev.nikdekur.ndkore.map.add
import dev.nikdekur.ndkore.scheduler.Scheduler
import dev.nikdekur.ndkore.scheduler.SchedulerTask
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.talker.Talker
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

class SchedulerTimeoutService(
    val scheduler: Scheduler
) : TimeoutService {

    val timeoutsMap: MutableListsMap<Address, SchedulerTask> = ConcurrentHashMap()

    override fun scheduleTimeout(
        talker: Talker,
        timeout: Duration,
        callback: suspend () -> Unit
    ) {
        val task = scheduler.runTaskLater(timeout) {
            callback()
        }
        timeoutsMap.add(talker.address, task, ::LinkedList)
    }

    override fun cancelTimeouts(talker: Talker) {
        timeoutsMap.remove(talker.address)?.forEach(SchedulerTask::cancel)
    }


}