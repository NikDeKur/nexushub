package org.ndk.nexushub.network.ratelimit

import org.ndk.nexushub.network.talker.Talker

interface RateLimiter {

    fun acquire(talker: Talker): Boolean
}