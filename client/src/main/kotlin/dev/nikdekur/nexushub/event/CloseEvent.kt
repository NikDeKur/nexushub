/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.event

import dev.nikdekur.nexushub.network.CloseCode

sealed class Close : Event() {

    /**
     * The Gateway was detached, all resources tied to the gateway should be freed.
     */
    object Detach : Close()

    /**
     * The user closed the Gateway connection.
     */
    object UserClose : Close()

    /**
     * The connection was closed because of a timeout, probably due to a loss of internet connection.
     */
    object Timeout : Close()

    /**
     * Server closed the connection with a [code] and [comment].
     *
     * @param recoverable true if the gateway will automatically try to reconnect.
     */
    data class ServerClose(val code: CloseCode, val comment: String, val recoverable: Boolean) : Close()

    /**
     *  The Gateway has failed to establish a connection too many times and will not try to reconnect anymore.
     *  The user is free to manually connect again using [Gateway.start], otherwise all resources linked to the Gateway should free and the Gateway [detached][Gateway.detach].
     */
    object RetryLimitReached : Close()
}