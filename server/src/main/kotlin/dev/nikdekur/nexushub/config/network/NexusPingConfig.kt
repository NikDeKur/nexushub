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
import net.mamoe.yamlkt.Comment

@Serializable
data class NexusPingConfig(
    @Comment("The interval in seconds between pings")
    val interval: Int,

    @Comment("The extra interval in milliseconds to wait before considering a node dead")
    @SerialName("extra_interval")
    val extraInterval: Int,
)