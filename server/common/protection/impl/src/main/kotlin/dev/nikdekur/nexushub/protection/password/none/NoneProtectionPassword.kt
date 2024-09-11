/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:OptIn(ExperimentalStdlibApi::class)

package dev.nikdekur.nexushub.protection.password.none

import dev.nikdekur.nexushub.protection.password.Password


data class NoneProtectionPassword(val password: String) : Password {

    override fun isEqual(password: String): Boolean {
        return this.password == password
    }

    override fun serialize(): String {
        return password
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NoneProtectionPassword) return false

        if (password != other.password) return false

        return true
    }

    override fun hashCode(): Int {
        return password.hashCode()
    }

    override fun toString(): String {
        return "NoneProtectionPassword(password='$password')"
    }

}