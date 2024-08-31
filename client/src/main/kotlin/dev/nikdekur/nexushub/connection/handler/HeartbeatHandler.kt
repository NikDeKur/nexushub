/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.connection.handler

import dev.nikdekur.ndkore.scheduler.Scheduler
import dev.nikdekur.ndkore.scheduler.SchedulerTask
import dev.nikdekur.ndkore.scheduler.runTaskTimer
import dev.nikdekur.nexushub.event.Close
import dev.nikdekur.nexushub.event.Event
import dev.nikdekur.nexushub.event.NetworkEvent
import dev.nikdekur.nexushub.event.NetworkEvent.HeartbeatACK
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.PacketHeartbeat
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class HeartbeatHandler(
    flow: Flow<Event>,
    private val send: suspend (Packet) -> Unit,
    private val ping: (Duration) -> Unit,
    private val scheduler: Scheduler,
    private val timeSource: TimeSource = TimeSource.Monotonic
) : Handler(flow, "HeartbeatHandler") {


    private var timestamp: TimeMark = timeSource.markNow()

    lateinit var task: SchedulerTask

    override fun start() {
        on<NetworkEvent.ReadyEvent> { ready ->
            val interval = ready.heartbeatInterval.milliseconds
            task = scheduler.runTaskTimer(interval) {
                timestamp = timeSource.markNow()
                send(PacketHeartbeat())
            }
        }

        on<HeartbeatACK> {
            ping(timestamp.elapsedNow())
        }

        on<Close> {
            if (::task.isInitialized) {
                task.cancel()
            }
        }
    }
}
