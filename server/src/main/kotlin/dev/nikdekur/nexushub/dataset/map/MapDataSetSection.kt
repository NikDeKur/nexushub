/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset.map

import dev.nikdekur.ndkore.ext.firstInstanceOrNull
import dev.nikdekur.nexushub.dataset.DataSetSection
import dev.nikdekur.nexushub.dataset.PropertyName
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

open class MapDataSetSection(
    val map: Map<String, Any>
) : DataSetSection {

    override fun getSection(key: String): DataSetSection? {
        @Suppress("UNCHECKED_CAST")
        val map = map[key] as? Map<String, Any> ?: return null
        return MapDataSetSection(map)
    }

    override operator fun <T : Any> get(key: String, clazz: KClass<T>): T? {
        val at = map[key] ?: return null
        @Suppress("UNCHECKED_CAST")
        if (clazz.isInstance(at)) return at as T

        val constructor = clazz.primaryConstructor
            ?: throw IllegalArgumentException("No primary constructor for `${clazz.qualifiedName}`")
        val args = constructor.parameters.associateWith { param ->
            val annotation = param.annotations.firstInstanceOrNull<PropertyName>()
            val key = annotation?.name ?: param.name
            map[key] ?: error("Missing value for `$key`")
        }
        return constructor.callBy(args)
    }

}