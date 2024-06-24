package org.ndk.nexushub.client.connection.retry;

import kotlin.time.Duration

/**
 * A strategy for retrying after after a failed action.
 */
public interface Retry {
    /**
     * Whether this strategy has any more retries left.
     */
    public val hasNext: Boolean

    /**
     * Resets the underlying retry counter if this Retry uses an maximum for consecutive [retryDuration] invocations.
     * This should be called after a successful [retryDuration].
     */
    public fun reset()

    /**
     * Returns the duration to wait before retrying.
     */
    fun retryDuration(): Duration
}
