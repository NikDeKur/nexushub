package dev.nikdekur.nexushub.connection.gateway

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import dev.nikdekur.nexushub.connection.retry.Retry
import dev.nikdekur.nexushub.event.Event

data class GatewayData(
    val host: String,
    val port: Int,
    val retry: Retry,
    val dispatcher: CoroutineDispatcher,
    val eventFlow: MutableSharedFlow<Event> = MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
)