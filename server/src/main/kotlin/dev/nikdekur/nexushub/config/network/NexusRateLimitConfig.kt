/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.config.network

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NexusRateLimitConfig(
    @YamlComment("The maximum number of requests that can be made in the time window")
    @SerialName("max_requests")
    val maxRequests: Int,

    @YamlComment("The time window in seconds")
    @SerialName("time_window")
    val timeWindow: Int
)