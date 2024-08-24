/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.service

import dev.nikdekur.ndkore.service.Service
import dev.nikdekur.nexushub.NexusHubServer

interface NexusHubService : Service<NexusHubServer>, NexusHubComponent {
    override val manager
        get() = app.servicesManager

    override fun doEnable() {
        // Remove standard error handling.
        // Any error is fatal for the server.
        onEnable()
    }

    override fun doDisable() {
        // Remove standard error handling.
        // Any error is fatal for the server.
        onDisable()
    }
}