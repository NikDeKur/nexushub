/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub

import kotlin.time.Duration

abstract class TestNexusHubServer : AbstractNexusHubServer() {



    override fun start() {
        super.start()

        logger.info("Starting NexusHub server...")
    }

    override fun stop(gracePeriod: Duration, timeout: Duration) {
        logger.info("Stopping test server...")
        logger.info("Oh, there is no server to stop. Ahahaha!")
    }
}