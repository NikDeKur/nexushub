package dev.nikdekur.nexushub.network.ratelimit

import dev.nikdekur.nexushub.network.talker.Talker

interface RateLimiter {

    fun acquire(talker: Talker): Boolean
}