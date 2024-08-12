/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.serialization

import dev.nikdekur.nexushub.scope.ScopeData
import dev.nikdekur.nexushub.sesion.Session
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer

/**
 * Implementation of the [DataSerializer] interface using JSON serialization.
 * This class provides methods to serialize and deserialize session data to and from JSON format.
 * It also allows checking if the data matches its default state.
 *
 * @param H the type of the session holder.
 * @param S the type of the session data, which must be serializable.
 * @param format the [StringFormat] used for encoding and decoding the session data.
 * @param serializer the [KSerializer] for the session data type [S].
 *
 * @constructor Creates a new instance of [JsonSerializer] with the provided format and serializer.
 *
 * @property format the [StringFormat] used for encoding and decoding session data.
 * @property serializer the [KSerializer] used to serialize and deserialize session data.
 *
 * ### Example Usage
 *
 * ```kotlin
 * val jsonFormat = Json { prettyPrint = true }
 * val sessionSerializer = JsonSerializer<SessionHolder, SessionData>(jsonFormat, SessionData.serializer())
 *
 * val sessionData = SessionData(...)  // create your session data instance
 * val session = ...  // create or obtain your session instance
 *
 * // Serialize session data to JSON
 * val jsonData = sessionSerializer.serialize(session, sessionData)
 *
 * // Deserialize JSON back to session data
 * val restoredData = sessionSerializer.deserialize(session, jsonData)
 *
 * // Check if session data is in its default state
 * val isDefault = sessionSerializer.isDefault(session, sessionData)
 * ```
 */
open class JsonSerializer<H, S : ScopeData<S>>(
    val format: StringFormat,
    val serializer: KSerializer<S>
) : DataSerializer<H, S> {

    /**
     * Serializes the provided session data to a JSON string using the specified [StringFormat].
     *
     * @param session the session to which the data belongs.
     * @param data the session data to be serialized.
     * @return a JSON [String] representing the serialized session data.
     */
    @OptIn(InternalSerializationApi::class)
    override suspend fun serialize(session: Session<H, S>, data: S): String {
        @Suppress("kotlin:S6530") // Serializer is always of type KSerializer<S>
        val serializer = data::class.serializer() as KSerializer<S>
        return format.encodeToString(serializer, data)
    }

    /**
     * Deserializes the provided JSON string into session data of type [S].
     *
     * @param session the session to which the data belongs.
     * @param dataJson the JSON string representing the session data.
     * @return the deserialized session data of type [S].
     */
    override suspend fun deserialize(session: Session<H, S>, dataJson: String): S {
        return format.decodeFromString(serializer, dataJson)
    }

    /**
     * Checks whether the provided session data is in its default state.
     * This is determined by deserializing an empty JSON object and comparing it to the provided data.
     *
     * @param session the session to which the data belongs.
     * @param data the session data to be checked.
     * @return `true` if the data is in its default state, `false` otherwise.
     */
    override suspend fun isDefault(session: Session<H, S>, data: S): Boolean {
        return data == deserialize(session, "{}")
    }
}
