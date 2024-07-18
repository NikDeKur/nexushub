/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.connection.retry

/**
 * A strategy for retrying after after a failed action.
 */
interface Retry {
    /**
     * Whether this strategy has any more retries left.
     */
    val hasNext: Boolean

    /**
     * Resets the underlying retry counter if this Retry uses an maximum for consecutive [retryDuration] invocations.
     * This should be called after a successful [retryDuration].
     */
    fun reset()


    suspend fun retry()
}
