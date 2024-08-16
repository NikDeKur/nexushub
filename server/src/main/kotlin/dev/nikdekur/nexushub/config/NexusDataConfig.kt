/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("PropertyName", "kotlin:S117")

package dev.nikdekur.nexushub.config

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable

@Serializable
data class NexusDataConfig(
    @YamlComment("Interval to clear cached holder data after write/access (in seconds)")
    val cache_expiration: Long = 300,
    @YamlComment("Maximum number of cached holder data by each scope")
    val cache_max_size: Long = 1000,
)