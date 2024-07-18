/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

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