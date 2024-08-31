/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.ratelimit

import dev.nikdekur.nexushub.dataset.LenientDurationSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Serializable
data class PeriodRateLimitDataSet(
    /**
     * The maximum number of requests that can be made in the time window
     */
    @SerialName("max_requests")
    val maxRequests: Long = 1000,


    /**
     * Time window to limit requests
     */
    @SerialName("time_window")
    @Serializable(LenientDurationSerializer::class)
    val timeWindow: Duration = 1.seconds
)