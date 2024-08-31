/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.talker

import dev.nikdekur.nexushub.util.CloseCode
import org.slf4j.LoggerFactory

abstract class AbstractTalker : Talker {


    val logger = LoggerFactory.getLogger(javaClass)

    override var isBlocked = false

    override val isOpen: Boolean
        get() = !isBlocked

    override suspend fun closeWithBlock(code: CloseCode, reason: String) {
        isBlocked = true
        close(code, reason)
    }
}