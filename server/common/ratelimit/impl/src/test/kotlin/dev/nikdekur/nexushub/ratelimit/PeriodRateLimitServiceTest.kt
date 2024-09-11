/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.ratelimit

import dev.nikdekur.ndkore.service.get
import dev.nikdekur.ndkore.test.realDelay
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.lightWeightNexusHubServer
import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.talker.NOOPTalker
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class PeriodRateLimitServiceTest {


    fun newLimiter(data: PeriodRateLimitDataSet): RateLimitService {
        val builder = { app: NexusHubServer -> PeriodRateLimitService(app, data) }
        val server = lightWeightNexusHubServer {
            service(builder, RateLimitService::class)
        }
        return server.get()
    }


    @Test
    fun `test simple rate limit`() = runTest {
        val data = PeriodRateLimitDataSet(2, 100.milliseconds)
        val limiter = newLimiter(data)
        val talker = NOOPTalker()

        assertTrue(limiter.acquire(talker))
        assertTrue(limiter.acquire(talker))
        assertFalse(limiter.acquire(talker))
    }


    @Test
    fun `test simple rate limit after period`() = runTest {
        val data = PeriodRateLimitDataSet(2, 100.milliseconds)
        val limiter = newLimiter(data)
        val talker = NOOPTalker()

        assertTrue(limiter.acquire(talker))
        assertTrue(limiter.acquire(talker))
        assertFalse(limiter.acquire(talker))
        realDelay(100)
        assertTrue(limiter.acquire(talker))
        assertTrue(limiter.acquire(talker))
        assertFalse(limiter.acquire(talker))
    }


    @Test
    fun `test simple rate limit after period with different talkers`() = runTest {
        val data = PeriodRateLimitDataSet(2, 100.milliseconds)
        val limiter = newLimiter(data)
        val talker1 = NOOPTalker(Address("test", 0))
        val talker2 = NOOPTalker(Address("test", 1))

        assertTrue(limiter.acquire(talker1))
        assertTrue(limiter.acquire(talker1))
        assertFalse(limiter.acquire(talker1))

        assertTrue(limiter.acquire(talker2))
        assertTrue(limiter.acquire(talker2))
        assertFalse(limiter.acquire(talker2))

        realDelay(100)

        assertTrue(limiter.acquire(talker1))
        assertTrue(limiter.acquire(talker1))
        assertFalse(limiter.acquire(talker1))

        assertTrue(limiter.acquire(talker2))
        assertTrue(limiter.acquire(talker2))
        assertFalse(limiter.acquire(talker2))
    }
}