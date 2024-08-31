/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.talker

import dev.nikdekur.nexushub.network.Address
import dev.nikdekur.nexushub.network.dsl.IncomingContext
import dev.nikdekur.nexushub.network.dsl.PacketReactionBuilder
import dev.nikdekur.nexushub.network.transmission.PacketTransmission
import dev.nikdekur.nexushub.packet.Packet
import dev.nikdekur.nexushub.util.CloseCode
import java.util.function.Predicate

/**
 * # Talker
 *
 * A talker is an entity that can send and receive packets from some source.
 * It used to speak with other entities.
 *
 * Either server or clients can be talkers.
 */
interface Talker {

    /**
     * The address of the talker.
     */
    val address: Address

    /**
     * Represent if the talker is open.
     *
     * Open talkers can send and receive packets.
     */
    val isOpen: Boolean

    /**
     * Send a packet.
     *
     * The Packet will be sent to the source the talker is speaking with.
     *
     * Will suspend until the send operation is complete.
     * To wait for a response, use [PacketTransmission.await].
     *
     * @param packet The packet to send.
     * @param reaction The reaction to the packet. It's a handler that will process the response received later.
     * @param respondTo The packet to respond to or null if not responding to any packet.
     * @return The transmission of the packet.
     */
    suspend fun <R> send(
        packet: Packet,
        reaction: PacketReactionBuilder<R> = {},
        respondTo: UShort?
    ): PacketTransmission<R>

    /**
     * Send a packet.
     *
     * The Packet will be sent to the source the talker is speaking with.
     *
     * Will suspend until the send operation is complete.
     * To wait for a response, use [PacketTransmission.await].
     *
     * @param packet The packet to send.
     * @param reaction The reaction to the packet. It's a handler that will process the response received later.
     * @return The transmission of the packet.
     */
    suspend fun <R> send(
        packet: Packet,
        reaction: PacketReactionBuilder<R> = {}
    ) = send<R>(packet, reaction, null)

    suspend fun receive(data: ByteArray): IncomingContext<Packet>?

    /**
     * Wait for a packet of the specified type.
     *
     * Suspends until a packet of the specified type is received.
     * Recommended to be used with [kotlinx.coroutines.withTimeout]
     *
     * @param packetClass The class of the packet.
     * @param condition The condition to check that the packet is the one you are waiting for.
     * @return The packet.
     */
    suspend fun <T : Packet> wait(
        packetClass: Class<T>,
        condition: Predicate<T> = Predicate<T> { true }
    ): IncomingContext<T>

    /**
     * Close the talker.
     *
     * Closed talkers cannot send or receive packets.
     *
     * Irreversible operation.
     *
     * @param code The close code.
     * @param comment The close comment.
     */
    suspend fun close(code: CloseCode, comment: String = "")

    /**
     * Represent if the talker is blocked from sending packets.
     *
     * Blocked talkers cannot send packets
     */
    val isBlocked: Boolean

    /**
     * Close the talker with blocking.
     *
     * This will block the talker from sending packets.
     *
     * @param code The close code.
     * @param reason The close reason.
     */
    suspend fun closeWithBlock(code: CloseCode, reason: String = "")
}

/**
 * Represent if the talker is closed.
 *
 * Closed talkers cannot send or receive packets.
 */
inline val Talker.isClosed: Boolean
    get() = !isOpen

@JvmName("sendUnit")
suspend inline fun Talker.send(
    packet: Packet,
    noinline reaction: PacketReactionBuilder<Unit> = {}
) = send<Unit>(packet, reaction)

suspend inline fun <reified T : Packet> Talker.wait(condition: Predicate<T> = Predicate<T> { true }) =
    wait(T::class.java, condition)