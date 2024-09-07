/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.ratelimit

import dev.nikdekur.nexushub.network.talker.Talker

/**
 * A service that provides rate limiting functionality.
 */
interface RateLimitService {

    /**
     * Acquire a rate limit token for the given talker.
     *
     * @param talker The talker to acquire the token for.
     * @return True if the token was acquired, false if the rate limit was exceeded.
     */
    fun acquire(talker: Talker): Boolean
}