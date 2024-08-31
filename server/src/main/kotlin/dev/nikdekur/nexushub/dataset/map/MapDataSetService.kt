/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)

package dev.nikdekur.nexushub.dataset.map

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.LenientDurationSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.time.Duration

class MapDataSetService(
    override val app: NexusHubServer,
    val map: Map<String, Any>
) : DataSetService {

    lateinit var json: Json
    lateinit var root: MapDataSetSection

    override fun onEnable() {
        json = Json {
            isLenient = true
            ignoreUnknownKeys = true

            @Suppress("UNCHECKED_CAST")
            serializersModule = SerializersModule {
                contextual(Duration::class, LenientDurationSerializer)
                contextual(Any::class, DynamicLookupSerializer)
                contextual(ArrayList::class, ListSerializer(DynamicLookupSerializer) as KSerializer<ArrayList<*>>)
                contextual(
                    HashMap::class,
                    MapSerializer(String::class.serializer(), DynamicLookupSerializer) as KSerializer<HashMap<*, *>>
                )
                contextual(
                    LinkedHashMap::class,
                    MapSerializer(
                        String::class.serializer(),
                        DynamicLookupSerializer
                    ) as KSerializer<LinkedHashMap<*, *>>
                )
            }
        }

        root = MapDataSetSection(json, map)
    }

    override fun getSection(key: String) = root.getSection(key)
    override fun <T : Any> get(key: String, clazz: KClass<T>) = root[key, clazz]
}