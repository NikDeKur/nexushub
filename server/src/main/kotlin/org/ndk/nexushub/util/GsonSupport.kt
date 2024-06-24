package org.ndk.nexushub.util

import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
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