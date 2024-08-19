/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

@file:Suppress("NOTHING_TO_INLINE")

package dev.nikdekur.nexushub.storage.request

import kotlin.reflect.KProperty

enum class CompOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS
}

data class Filter(
    val key: String,
    val operator: CompOperator,
    val value: Any
)


inline infix fun String.eq(value: Any) = Filter(this, CompOperator.EQUALS, value)
inline infix fun String.ne(value: Any) = Filter(this, CompOperator.NOT_EQUALS, value)
inline infix fun String.gt(value: Any) = Filter(this, CompOperator.GREATER_THAN, value)
inline infix fun String.lt(value: Any) = Filter(this, CompOperator.LESS_THAN, value)
inline infix fun String.gte(value: Any) = Filter(this, CompOperator.GREATER_THAN_OR_EQUALS, value)
inline infix fun String.lte(value: Any) = Filter(this, CompOperator.LESS_THAN_OR_EQUALS, value)


inline infix fun KProperty<*>.eq(value: Any) = Filter(name, CompOperator.EQUALS, value)
inline infix fun KProperty<*>.ne(value: Any) = Filter(name, CompOperator.NOT_EQUALS, value)
inline infix fun KProperty<*>.gt(value: Any) = Filter(name, CompOperator.GREATER_THAN, value)
inline infix fun KProperty<*>.lt(value: Any) = Filter(name, CompOperator.LESS_THAN, value)
inline infix fun KProperty<*>.gte(value: Any) = Filter(name, CompOperator.GREATER_THAN_OR_EQUALS, value)
inline infix fun KProperty<*>.lte(value: Any) = Filter(name, CompOperator.LESS_THAN_OR_EQUALS, value)
