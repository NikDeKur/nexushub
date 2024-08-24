/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.access

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class PingDataSet(

    /**
     * The interval to ping the node
     */
    val interval: Duration = 5.seconds,

    /**
     * The extra interval to wait for the node to respond
     *
     * Sum of [interval] and [extraInterval] is the total time to wait for the node to respond
     * before closing the connection
     */
    val extraInterval: Duration = 3.seconds
)