/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.protection

/**
 * # Password
 *
 * Interface representing the basic functionality of a password.
 */
interface Password {

    /**
     * Check if the password is equal to the given string.
     *
     * Might take some time to compute depending on the implementation.
     *
     * @param password The password to compare to
     * @return True if the password is equal to the given string, false otherwise
     */
    fun isEqual(password: String): Boolean

    /**
     * Serialize the password to a string.
     *
     * This string should be able to be used to recreate the password using [ProtectionService.deserializePassword].
     *
     * Distinct from [toString] which is for debugging purposes.
     */
    fun serialize(): String
}