/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.auth.password

data class Salt(
    val byte: ByteArray
) {

    @OptIn(ExperimentalStdlibApi::class)
    val hex = byte.toHEX()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Salt

        return byte.contentEquals(other.byte)
    }

    override fun hashCode(): Int {
        return byte.contentHashCode()
    }
}
