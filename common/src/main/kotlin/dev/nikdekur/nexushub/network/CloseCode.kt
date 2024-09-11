/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

enum class CloseCode(
    val retry: Boolean,
    val allowRespond: Boolean,
    val hardCode: Short? = null
) {

    // Client Code
    NORMAL(true, false, 1000),
    TIMEOUT(true, false),
    ALREADY_AUTHENTICATED(true, false),
    WRONG_CREDENTIALS(false, false),
    INVALID_DATA(false, false),
    NODE_ALREADY_EXISTS(false, false),
    AUTHENTICATION_TIMEOUT(true, false),
    NOT_AUTHENTICATED(true, false),
    UNEXPECTED_BEHAVIOUR(true, false),
    PING_FAILED(true, false),
    RATE_LIMITED(true, false),
    INTERNAL_ERROR(true, false),
    SHUTDOWN(true, true),
    ;


    val code = hardCode ?: (4000 + ordinal).toShort()

    companion object {
        fun fromCode(code: Short): CloseCode? {
            if (code >= 4000)
                return entries.getOrNull(code - 4000)
            return entries.firstOrNull { it.code == code }
        }
    }
}