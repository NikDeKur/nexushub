/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.protection

/**
 * # Protection Service
 *
 * The protection service is responsible for creating and managing passwords.
 * Different implementations of this service can provide different levels of security
 * or either no security at all.
 */
interface ProtectionService {

    /**
     * Creates a password from a string.
     *
     * May take some time to complete, depending on the implementation.
     *
     * @param string The string to create the password from.
     * @return The created password.
     */
    fun createPassword(string: String): Password

    /**
     * Deserializes a password from a string.
     *
     * May take some time to complete, depending on the implementation.
     *
     * String is guaranteed to be a result of a previous call to [Password.serialize].
     *
     * @param string The string to deserialize the password from.
     * @return The deserialized password.
     */
    fun deserializePassword(string: String): Password

    /**
     * Imitates encryption.
     *
     * Used for security, when the actual encryption is unnecessary.
     * It Should take some time to complete, depending on the implementation.
     */
    suspend fun imitateEncryption()
}