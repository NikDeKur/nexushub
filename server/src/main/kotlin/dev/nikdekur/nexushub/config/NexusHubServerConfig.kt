/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.config

import dev.nikdekur.nexushub.config.network.NexusNetworkConfig
import kotlinx.serialization.Serializable

@Serializable
data class NexusHubServerConfig(
    val network: NexusNetworkConfig,
    val data: NexusDataConfig,
    val database: NexusDatabaseConfig
)