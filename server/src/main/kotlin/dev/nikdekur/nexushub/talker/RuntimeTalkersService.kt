/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.talker

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.node.NodesService
import dev.nikdekur.nexushub.service.NexusHubService
import dev.nikdekur.nexushub.session.SessionsService
import org.koin.core.component.inject
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class RuntimeTalkersService(
    override val app: NexusHubServer
) : NexusHubService, TalkersService {

    val logger = LoggerFactory.getLogger(javaClass)

    val nodesService: NodesService by inject()
    val sessionsService: SessionsService by inject()

    val talkers = ConcurrentHashMap<Int, Talker>()

    override fun getExistingTalker(address: Int): Talker? {
        return talkers[address]
    }

    override fun setTalker(address: Int, talker: Talker) {
        talkers[address] = talker
    }

    override fun removeTalker(talker: Int) {
        talkers.remove(talker)
    }


    override fun cleanUp(address: Int) {
        getExistingTalker(address)?.let { talker ->
            val node = nodesService.getAuthenticatedNode(talker)
            if (node != null) {
                logger.info("[${talker.addressStr}] Node ${node.id} disconnected")
                nodesService.removeNode(node)
                sessionsService.stopAllSessions(node)
            }

            removeTalker(address)
        }
    }
}