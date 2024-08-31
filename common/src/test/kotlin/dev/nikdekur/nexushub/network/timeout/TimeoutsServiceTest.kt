/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.timeout

import dev.nikdekur.nexushub.network.talker.NOOPTalker
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds


interface TimeoutsServiceTest {

    val service: TimeoutService


    @Test
    fun `test correct timeout scheduling`() {
        val testTalker = NOOPTalker()

        var calledAt: Long? = null

        val requestTime = System.currentTimeMillis()

        service.scheduleTimeout(testTalker, 50.milliseconds) {
            calledAt = System.currentTimeMillis()
        }

        Thread.sleep(100)
        assertNotNull(calledAt != null) { "Timeout was not called" }

        val duration = calledAt!! - requestTime

        assert(duration >= 50 && duration < 100)
    }

    @Test
    fun `test canceling timeouts`() {
        val testTalker = NOOPTalker()

        var calledAt: Long? = null
        service.scheduleTimeout(testTalker, 50.milliseconds) {
            calledAt = System.currentTimeMillis()
        }

        Thread.sleep(25)

        service.cancelTimeouts(testTalker)

        Thread.sleep(50)

        assert(calledAt == null) { "Timeout was called" }
    }
}