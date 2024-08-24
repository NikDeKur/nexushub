/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.boot

import dev.nikdekur.nexushub.dataset.PropertyName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ShutdownDataSet(
    /**
     * Grace period to wait for all connections to close before shutting down
     */
    @PropertyName("grace_period")
    val gracePeriod: Duration = 10.seconds,


    /**
     * Timeout to wait for all connections to close before shutting down
     */
    val timeout: Duration = 10.seconds,
)
