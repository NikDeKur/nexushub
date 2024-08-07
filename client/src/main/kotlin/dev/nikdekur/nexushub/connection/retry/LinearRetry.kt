/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.connection.retry

import dev.nikdekur.ndkore.ext.trace
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.times

private val linearRetryLogger = LoggerFactory.getLogger("LinearRetry")

/**
 * A Retry that linearly increases the delay time between a given minimum and maximum over a given amount of tries.
 *
 * @param firstBackoff the initial delay for a [retryDuration] invocation.
 * @param maxBackoff the maximum delay for a [retryDuration] invocation.
 * @param maxTries the maximum number of consecutive retries before [hasNext] returns false.
 */
public class LinearRetry(
    private val firstBackoff: Duration,
    private val maxBackoff: Duration,
    private val maxTries: Int
) : Retry {

    init {
        require(firstBackoff.isPositive()) { "firstBackoff needs to be positive but was ${firstBackoff.inWholeMilliseconds} ms" }
        require(maxBackoff.isPositive()) { "maxBackoff needs to be positive but was ${maxBackoff.inWholeMilliseconds} ms" }
        require(
            maxBackoff.minus(firstBackoff).isPositive()
        ) { "maxBackoff ${maxBackoff.inWholeMilliseconds} ms needs to be bigger than firstBackoff ${firstBackoff.inWholeMilliseconds} ms" }
        require(maxTries > 1) { "maxTries needs to be greater than 1 but was $maxTries" }
    }

    private val tries = atomic(0)

    override val hasNext: Boolean
        get() = tries.value < maxTries

    override fun reset() {
        tries.update { 0 }
    }

    override suspend fun retry() {
        if (!hasNext) error("max retries exceeded")

        // tries/maxTries ratio * (backOffDiff) = retryProgress
        val ratio = tries.getAndIncrement() / (maxTries - 1).toDouble()
        val retryProgress = ratio * (maxBackoff - firstBackoff)
        val diff = firstBackoff + retryProgress

        linearRetryLogger.trace { "retry attempt ${tries.value}, delaying for $diff" }
        delay(diff)
    }

}
