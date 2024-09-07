/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.ping

import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.ndkore.scheduler.runTaskTimer
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.access.PingDataSet
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.get
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.util.CloseCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlin.properties.Delegates
import kotlin.time.Duration

class SchedulerPingService(
    override val app: NexusHubServer
) : NexusHubService(), PingService {


    val datasetService: DataSetService by inject()
    val nodesService: NodesService by inject()

    lateinit var scheduler: CoroutineScheduler

    override var pingInterval: Duration by Delegates.notNull()

    override fun onEnable() {
        scheduler = CoroutineScheduler.fromSupervisor(Dispatchers.Default)

        val config = datasetService.get<PingDataSet>("ping") ?: PingDataSet()
        pingInterval = config.interval
        val deadInterval = pingInterval + config.extraInterval

        scheduler.runTaskTimer(deadInterval) {
            nodesService.nodes.forEach { node ->
                if (node.isAlive(deadInterval)) return@forEach
                node.close(CloseCode.PING_FAILED, "Ping failed")
            }
        }
    }

    override fun onDisable() {
        scheduler.cancelAllTasks()
        scheduler.cancel()
    }
}