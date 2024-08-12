/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.connection

import dev.nikdekur.nexushub.NexusHub
import dev.nikdekur.nexushub.event.Close
import dev.nikdekur.nexushub.event.NetworkEvent
import org.slf4j.LoggerFactory

class NexusHubHandler(
    val hub: NexusHub
) {

    val logger = LoggerFactory.getLogger("NexusHubHandler")

    init {
        hub.on<NetworkEvent> {
            if (context.isResponse) return@on
            when (this) {
                is NetworkEvent.ScopeEvent -> {
                    val service = hub.getService(scopeId) ?: return@on
                    service.onEvent(this)
                }

                else -> {
                    // ignore
                }
            }
        }

        hub.on<NetworkEvent.ReadyEvent> {
            logger.info("NexusHub is ready")
        }

        hub.on<Close.ServerClose> {
            if (!code.allowRespond) return@on
            hub.stopServices()
        }
    }
}