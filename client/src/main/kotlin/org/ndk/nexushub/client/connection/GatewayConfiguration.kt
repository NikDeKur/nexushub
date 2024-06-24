package org.ndk.nexushub.client.connection

import kotlinx.coroutines.CoroutineDispatcher
import org.ndk.nexushub.client.connection.retry.Retry

data class GatewayConfiguration(
    val host: String,
    val port: Int,
    val retry: Retry,
    val dispatcher: CoroutineDispatcher,
)