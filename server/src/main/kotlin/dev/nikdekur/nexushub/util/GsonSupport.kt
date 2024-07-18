/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.util

import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import dev.nikdekur.nexushub.util.NexusData
import java.lang.reflect.Type


object GsonSupport {

    val gson = GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
        .create()

    fun dataToString(data: NexusData): String {
        return gson.toJson(data)
    }

    val nexusDataType: Type = object : TypeToken<NexusData>() {}.type
    fun dataFromString(json: String): NexusData {
        return gson.fromJson(json, nexusDataType)
    }
}