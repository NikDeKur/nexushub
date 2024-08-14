/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.config.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class NexusHubShutdownConfig(
    @SerialName("grace_period")
    val gracePeriod: Long = 5000L,


    val timeout: Long = 10000L,


    @SerialName("unit")
    private val _unit: String = "MILLISECONDS"
) {

    val unit: TimeUnit
        get() = TimeUnit.valueOf(_unit.uppercase())
}