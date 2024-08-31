/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.access

import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.service.NexusHubService

/**
 * # Access Service
 *
 * The access service is responsible for handling incoming data from clients and managing their access to the server.
 * This includes rate limiting, authentication, and handling of incoming data and further business logic and responses.
 */
interface AccessService : NexusHubService {

    /**
     * Receive data from a client talker.
     *
     * Returns the result of the receiving process that might be useful to caller function.
     *
     * @param talker The client talker.
     * @param data The data received.
     * @return The result of receiving the data.
     * @see ReceiveResult
     */
    suspend fun receiveData(talker: Talker, data: ByteArray): ReceiveResult

    /**
     * The result of receiving data from a client talker.
     *
     * This result is recommended to be handled, for example, destroying the talker if rate limited.
     * This is not necessary, [AccessService] will work anyway,
     * but caller may optimize the process by decreasing packet processing time.
     */
    sealed interface ReceiveResult {
        object RateLimited : ReceiveResult
        object InvalidData : ReceiveResult
        object Ok : ReceiveResult
    }

    /**
     * Called when the client talker connection is established and ready to process data.
     *
     * Implementation might start sending data to the client or start the authentication process.
     *
     * @param talker The client talker.
     * @return true if the talker is ready to process data, false if the talker closed.
     */
    suspend fun onReady(talker: Talker): Boolean

    /**
     * Called when the client talker connection is closed.
     *
     * Implementation might clean up resources or notify other services.
     *
     * @param talker The client talker.
     */
    suspend fun onClose(talker: Talker)
}