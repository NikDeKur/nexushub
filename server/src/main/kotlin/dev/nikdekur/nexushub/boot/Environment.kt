/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.boot

/**
 * Represents the environment of the application.
 *
 * The boot environment is a key-value store that contains
 * the configuration of the application at startup.
 */
interface Environment {

    /**
     * Gets the value of the specified key.
     *
     * @param key the key
     * @return the value, or `null` if the key is not present
     */
    fun getValue(key: String): String?

    /**
     * Request a value to be entered.
     *
     * @param key the key shortly describing the value
     * @return the value, or `null` if the value can't be requested
     */
    fun requestValue(key: String, description: String): String?
}