/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.timeout

import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.nexushub.network.talker.NOOPTalker
import kotlinx.coroutines.Dispatchers
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class SchedulerTimeoutServiceTest : TimeoutsServiceTest {

    override val service = SchedulerTimeoutService(
        CoroutineScheduler.fromSupervisor(Dispatchers.IO)
    )


    @Test
    fun `test internal registration`() {
        val testTalker = NOOPTalker()

        service.scheduleTimeout(testTalker, 50.milliseconds) {
            // Do nothing
        }

        assertContains(service.timeoutsMap, testTalker.address)
    }

    @Test
    fun `test internal unregistration`() {
        val testTalker = NOOPTalker()

        service.scheduleTimeout(testTalker, 50.milliseconds) {
            // Do nothing
        }

        service.cancelTimeouts(testTalker)

        assertTrue(service.timeoutsMap.isEmpty())
    }
}