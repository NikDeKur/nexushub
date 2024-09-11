/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package dev.nikdekur.nexushub.dataset.map

import dev.nikdekur.ndkore.ext.toBooleanSmartOrNull
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

object DynamicLookupSerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = ContextualSerializer(Any::class, null, emptyArray()).descriptor

    override fun serialize(encoder: Encoder, value: Any) {
        val actualSerializer = encoder.serializersModule.getContextual(value::class) ?: value::class.serializer()
        @Suppress("UNCHECKED_CAST")
        encoder.encodeSerializableValue(actualSerializer as KSerializer<Any>, value)
    }

    override fun deserialize(decoder: Decoder): Any {
        error("Unsupported")
    }
}

open class MapDataSetSection(
    val json: Json,
    val map: Map<String, Any>
) : dev.nikdekur.nexushub.dataset.DataSetSection {


    override fun getSection(key: String): dev.nikdekur.nexushub.dataset.DataSetSection? {
        @Suppress("UNCHECKED_CAST")
        val map = map[key] as? Map<String, Any> ?: return null
        return MapDataSetSection(json, map)
    }


    override operator fun <T : Any> get(key: String, clazz: KClass<T>): T? {
        val at = map[key] ?: return null
        @Suppress("UNCHECKED_CAST")
        if (clazz.isInstance(at)) return at as T
        @Suppress("UNCHECKED_CAST")
        when (clazz) {
            String::class -> return at.toString() as T
            Byte::class -> return at.toString().toByteOrNull() as T
            Short::class -> return at.toString().toShortOrNull() as T
            Int::class -> return at.toString().toIntOrNull() as T
            Long::class -> return at.toString().toLongOrNull() as T
            Float::class -> return at.toString().toFloatOrNull() as T
            Double::class -> return at.toString().toDoubleOrNull() as T
            Boolean::class -> return at.toString().toBooleanSmartOrNull() as T
        }

        val serialized = json.encodeToString(at)
        return Json.decodeFromString(clazz.serializer(), serialized)
    }
}