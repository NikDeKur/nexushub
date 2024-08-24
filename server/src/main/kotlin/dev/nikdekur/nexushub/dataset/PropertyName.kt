/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset

/**
 * # PropName
 *
 * Annotation to specify the name of the property in the data set
 *
 * For example, in config name style is snake_case, but in code it is camelCase.
 * In this case, you can use this annotation to specify the name of the property in the data set.
 *
 * If no annotation is present, the name of the property in the data set is the same as the name of the property in the code.
 *
 * @param name The name of the property in the data set
 */
annotation class PropertyName(
    val name: String
)
