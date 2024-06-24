package org.ndk.nexushub.network.ratelimit

import org.ndk.nexushub.network.talker.Talker
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.DurationUnit

data class PeriodRateLimiter(
    val limit: Int,
    val period: Duration
) : RateLimiter {

    private val map = ConcurrentHashMap<Int, Entry>()

    val periodMs = period.toLong(DurationUnit.MILLISECONDS)


    override fun acquire(talker: Talker): Boolean {
        val addressHash = talker.addressHash
        val now = System.currentTimeMillis()


        val entry = map.computeIfAbsent(addressHash) {
            Entry(addressHash, now, 0)
        }

        if (now - entry.start > periodMs) {
            map[addressHash] = Entry(addressHash, now, 0)
        }

        if (entry.count >= limit) {
            return false
        }

        entry.count++
        return true
    }



    data class Entry(
        val addressHash: Int,
        val start: Long,
        var count: Int
    )
}