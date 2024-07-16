package dev.nikdekur.nexushub.network.ratelimit

import dev.nikdekur.nexushub.network.talker.Talker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.DurationUnit

data class PeriodRateLimiter(
    val limit: Int,
    val period: Duration
) : RateLimiter {

    private val map = ConcurrentHashMap<Int, Entry>()
    private val periodMs = period.toLong(DurationUnit.MILLISECONDS)

    override fun acquire(talker: Talker): Boolean {
        val addressHash = talker.addressHash
        val now = System.currentTimeMillis()
        var limitExceeded = false

        map.compute(addressHash) { _, existingEntry ->
            existingEntry?.let {
                if (now - it.start > periodMs) {
                    Entry(addressHash, now, AtomicInteger(1))
                } else {
                    if (it.count.incrementAndGet() > limit) {
                        limitExceeded = true
                        it
                    } else {
                        it
                    }
                }
            } ?: Entry(addressHash, now, AtomicInteger(1))
        }

        return !limitExceeded
    }

    data class Entry(
        val addressHash: Int,
        val start: Long,
        val count: AtomicInteger
    )
}


//fun main() {
//
//    // Small test
//    val limiter = PeriodRateLimiter(2, 2.seconds)
//
//    val talker = object : Talker {
//        override val addressHash: Int = 0
//        override val addressStr: String
//            get() = TODO("Not yet implemented")
//        override val isOpen: Boolean
//            get() = TODO("Not yet implemented")
//
//        override suspend fun send(transmission: PacketTransmission<*>) {
//            TODO("Not yet implemented")
//        }
//
//        override suspend fun receive(data: ByteArray): IncomingContext<Packet>? {
//            TODO("Not yet implemented")
//        }
//
//        override suspend fun close(code: Short, reason: String) {
//            TODO("Not yet implemented")
//        }
//    }
//
//    println(limiter.acquire(talker))
//    println(limiter.acquire(talker))
//    println(limiter.acquire(talker))
//
//    Thread.sleep(1000)
//
//    println(limiter.acquire(talker))
//}
