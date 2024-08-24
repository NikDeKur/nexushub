/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.ratelimit

import dev.nikdekur.nexushub.dataset.PropertyName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PeriodRateLimitDataSet(
    /**
     * The maximum number of requests that can be made in the time window
     */
    @PropertyName("max_requests")
    val maxRequests: Long = 1000,


    /**
     * Time window to limit requests
     */
    @PropertyName("time_window")
    val timeWindow: Duration = 1.seconds
)