/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

import dev.nikdekur.ndkore.scheduler.impl.CoroutineScheduler
import dev.nikdekur.nexushub.network.talker.NOOPTalker
import dev.nikdekur.nexushub.network.timeout.SchedulerTimeoutService
import kotlinx.coroutines.Dispatchers

class RuntimePacketControllerTest : PacketControllerTest {

    val timeoutService = SchedulerTimeoutService(
        CoroutineScheduler.fromSupervisor(Dispatchers.IO)
    )

    override fun newController(): PacketController {
        return RuntimePacketController(NOOPTalker(), timeoutService)
    }
}