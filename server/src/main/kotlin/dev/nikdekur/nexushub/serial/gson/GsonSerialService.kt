/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.serial.gson

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.serial.SerialService
import dev.nikdekur.nexushub.util.NexusData
import java.lang.reflect.Type

class GsonSerialService(
    override val app: NexusHubServer
) : SerialService {

    lateinit var gson: Gson
    lateinit var nexusDataType: Type


    override fun onEnable() {
        gson = GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create()

        nexusDataType = object : TypeToken<NexusData>() {}.type
    }

    override fun serialize(data: NexusData): String {
        return gson.toJson(data)
    }

    override fun deserialize(data: String): NexusData {
        return gson.fromJson(data, nexusDataType)
    }
}