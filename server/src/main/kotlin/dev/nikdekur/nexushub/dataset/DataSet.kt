/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset

import java.util.concurrent.TimeUnit

interface DataSet {

    val network: Network
    val cache: Cache

    interface Network {
        val port: Int
        val ssl: SSL?
        val ping: Ping
        val rateLimit: RateLimit
        val shutdown: Shutdown

        interface SSL {
            val cert: String
            val key: String
        }

        interface Ping {
            val interval: Int
            val extraInterval: Int
        }

        interface RateLimit {
            /**
             * The maximum number of requests that can be made in the time window
             */
            val maxRequests: Int

            /**
             * Time window to limit requests in seconds
             */
            val timeWindow: Int
        }

        interface Shutdown {
            /**
             * Grace period to wait for all connections to close before shutting down in specified unit
             */
            val gracePeriod: Long

            /**
             * Timeout to wait for all connections to close before shutting down in specified unit
             */
            val timeout: Long

            /**
             * Unit of grace period and timeout
             */
            val unit: TimeUnit
        }
    }

    interface Cache {
        /**
         * Interval to clear cached holder data after write/access (in seconds)
         */
        val cacheExpiration: Long

        /**
         * Maximum number of cached holder data by each scope
         */
        val cacheMaxSize: Long
    }
}