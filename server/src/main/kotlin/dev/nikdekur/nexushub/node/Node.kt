/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.node

import dev.nikdekur.ndkore.`interface`.Snowflake
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.talker.ClientTalker
import kotlin.time.Duration

/**
 * # Node
 *
 * Represents a Node connected to NexusHub server, which can send and receive packets
 */
interface Node : ClientTalker, Snowflake<String> {

    /**
     * Check if the node is alive
     *
     * The [deadInterval] is interval before last call to [isAlive] to consider the node dead
     * and should be removed.
     *
     * @param deadInterval interval to consider the node dead
     * @return true if the node is alive, false otherwise
     */
    fun isAlive(deadInterval: Duration): Boolean

    /**
     * Process the incoming packet
     *
     * Should perform the necessary actions based on the incoming packet
     * and respond if necessary
     *
     * @param context incoming context
     */
    suspend fun processPacket(context: IncomingContext<out Packet>)
}
