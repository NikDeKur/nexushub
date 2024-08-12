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


/**
 * Interface representing a generic data serializer for session data. This interface defines the
 * contract for serializing and deserializing session data, as well as checking if the data
 * matches its default state.
 *
 * @param H the type of the session holder.
 * @param S the type of the session data.
 */
interface DataSerializer<H, S : ScopeData<S>> {

    /**
     * Serializes the provided session data into a JSON string or another string format.
     *
     * @param session the session to which the data belongs.
     * @param data the session data to be serialized.
     * @return a [String] representation of the serialized session data.
     */
    suspend fun serialize(session: Session<H, S>, data: S): String


    /**
     * Deserializes the provided JSON string into session data.
     *
     * @param session the session to which the data belongs.
     * @param dataJson the JSON string representing the session data.
     * @return the deserialized session data of type [S].
     */
    suspend fun deserialize(session: Session<H, S>, dataJson: String): S


    /**
     * Checks whether the provided session data is in its default state.
     *
     * @param session the session to which the data belongs.
     * @param data the session data to be checked.
     * @return `true` if the data is in its default state, `false` otherwise.
     */
    suspend fun isDefault(session: Session<H, S>, data: S): Boolean
}
