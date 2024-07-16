package dev.nikdekur.nexushub.connection.handler

import dev.nikdekur.ndkore.scheduler.Scheduler
import dev.nikdekur.ndkore.scheduler.SchedulerTask
import kotlinx.coroutines.flow.Flow
import dev.nikdekur.nexushub.event.Close
import dev.nikdekur.nexushub.event.Event
import dev.nikdekur.nexushub.event.NetworkEvent
import dev.nikdekur.nexushub.event.NetworkEvent.HeartbeatACK
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.packet.`in`.PacketHeartbeat
import kotlin.time.Duration
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
            val interval = ready.heartbeatInterval.toLong()
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
