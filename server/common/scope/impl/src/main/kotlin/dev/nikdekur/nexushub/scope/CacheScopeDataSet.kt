/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.scope

import dev.nikdekur.ndkore.ext.LenientDurationSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
data class CacheScopeDataSet(
    /**
     * Interval to clear cached holder data after write/access
     */
    @SerialName("cache_expiration")
    @Serializable(LenientDurationSerializer::class)
    val cacheExpiration: Duration = 5.minutes,

    /**
     * Maximum number of cached holder data by each scope
     */
    @SerialName("cache_max_size")
    val cacheMaxSize: Long = 1000
)