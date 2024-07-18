/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.talker

import dev.nikdekur.nexushub.network.talker.Talker
import dev.nikdekur.nexushub.util.CloseCode

interface ClientTalker : Talker {

    /**
     * Represent if the talker is blocked from sending packets.
     *
     * The server could set this to ignore any packets after rate limiting or other reasons.
     */
    val isBlocked: Boolean

    suspend fun closeWithBlock(code: CloseCode, reason: String = "")
}