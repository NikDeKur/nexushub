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

enum class Order {
    ASCENDING,
    DESCENDING
}

data class Sort(
    val field: String,
    val order: Order
)

inline fun KProperty<*>.desc() = Sort(name, Order.DESCENDING)
inline fun KProperty<*>.asc() = Sort(name, Order.ASCENDING)
