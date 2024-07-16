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
