/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.serial

import dev.nikdekur.nexushub.util.NexusData

/**
 * # Serial (Serializing) Service
 *
 * Service for serializing and deserializing NexusData objects.
 *
 */
interface SerialService {

    /**
     * Serializes the given NexusData to a string.
     *
     * The string should be able to be deserialized back into the NexusData object.
     *
     * @param data The NexusData object to serialize.
     * @return The serialized string.
     */
    fun serialize(data: NexusData): String

    /**
     * Deserializes the given string into a NexusData object.
     *
     * The string should have been serialized from a NexusData object.
     *
     * @param data The serialized string.
     * @return The deserialized NexusData object.
     */
    fun deserialize(data: String): NexusData
}