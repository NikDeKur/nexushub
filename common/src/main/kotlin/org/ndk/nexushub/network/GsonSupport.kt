package org.ndk.nexushub.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


typealias NexusData = Map<String, Any>

object GsonSupport {

    val gson = Gson()

    fun dataToString(data: NexusData): String {
        return gson.toJson(data)
    }

    val nexusDataType: Type = object : TypeToken<NexusData>() {}.type
    fun dataFromString(json: String): NexusData {
        return gson.fromJson(json, nexusDataType)
    }
}