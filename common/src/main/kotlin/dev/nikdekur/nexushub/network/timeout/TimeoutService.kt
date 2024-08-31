/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.timeout

import dev.nikdekur.nexushub.network.talker.Talker
import kotlin.time.Duration

/**
 * # TimeoutService
 *
 * A service that manages timeouts for requests.
 *
 * More like an internal service for the network layer hid by implementations.
 */
interface TimeoutService {

    /**
     * Schedules a timeout for a talker.
     *
     * Callback is called when the timeout is reached.
     * To cancel the timeout, use [cancelTimeouts] and callback will not be called.
     *
     * @param talker The talker to schedule the timeout for.
     * @param timeout The duration of the timeout.
     * @param callback The callback to be called when the timeout is reached.
     */
    fun scheduleTimeout(talker: Talker, timeout: Duration, callback: suspend () -> Unit)

    /**
     * Cancels all timeouts for a talker.
     *
     * The callback for any timeouts will not be called.
     *
     * If the talker has no timeouts scheduled, this function does nothing.
     *
     * @param talker The talker to cancel the timeouts for.
     */
    fun cancelTimeouts(talker: Talker)
}