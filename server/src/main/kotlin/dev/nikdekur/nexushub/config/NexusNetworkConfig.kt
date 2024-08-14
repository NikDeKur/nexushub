/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.config

import dev.nikdekur.nexushub.config.network.NexusHubSSLConfig
import dev.nikdekur.nexushub.config.network.NexusHubShutdownConfig
import dev.nikdekur.nexushub.config.network.NexusPingConfig
import dev.nikdekur.nexushub.config.network.NexusRateLimitConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NexusNetworkConfig(
    val port: Int,


    val ssl: NexusHubSSLConfig? = null,


    val ping: NexusPingConfig,


    @SerialName("rate_limit")
    val rateLimit: NexusRateLimitConfig,


    val shutdown: NexusHubShutdownConfig
)