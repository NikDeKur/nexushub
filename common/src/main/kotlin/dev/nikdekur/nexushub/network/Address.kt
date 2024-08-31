/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network

/**
 * Represents a network address.
 *
 * @property host The host.
 * @property port The port.
 */
data class Address(
    val host: String,
    val port: Int
) {

    /**
     * Returns the string representation of the address.
     */
    override fun toString() = "$host:$port"
}