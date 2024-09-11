/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.boot

import dev.nikdekur.ndkore.ext.input

/**
 * A boot environment that is backed by a map.
 *
 * @param values the map
 * @constructor creates a new boot environment
 */
class ConsoleBootEnvironment(
    val values: Map<String, String>
) : Environment {

    override fun getValue(key: String): String? {
        return values[key]
    }

    override fun requestValue(key: String, description: String): String? {
        return input(description)
    }


    companion object {


        /**
         * Parses command line arguments into a [dev.nikdekur.nexushub.boot.Environment].
         *
         * The arguments should be in the form of `key=value` pairs.
         *
         * @param args the command line arguments
         * @return the boot environment
         */
        @JvmStatic
        fun fromCommandLineArgs(args: Array<String>): Environment {
            if (args.isEmpty())
                return ConsoleBootEnvironment(emptyMap())

            val values = args
                .map { it.split("=") }
                .onEach {
                    require(it.size == 2) { "Invalid command line argument: `$it`" }
                }
                .associate { it[0] to it[1] }

            return ConsoleBootEnvironment(values)
        }
    }
}