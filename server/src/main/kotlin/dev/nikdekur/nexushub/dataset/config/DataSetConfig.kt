/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset.config

import com.charleskorn.kaml.YamlComment
import dev.nikdekur.nexushub.dataset.DataSet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit

@Serializable
data class DataSetConfig(
    override val network: Network,
    override val cache: Cache,
    override val ping: Ping,
    override val shutdown: Shutdown
) : DataSet {

    @Serializable
    class Network(
        override val port: Int,
        override val ssl: SSL? = null,

        @SerialName("rate_limit")
        override val rateLimit: RateLimit,
    ) : DataSet.Network {

        @Serializable
        data class SSL(
            override val cert: String,
            override val key: String
        ) : DataSet.Network.SSL


        @Serializable
        data class RateLimit(
            @YamlComment("The maximum number of requests that can be made in the time window")
            @SerialName("max_requests")
            override val maxRequests: Int,

            @YamlComment("The time window in seconds")
            @SerialName("time_window")
            override val timeWindow: Int
        ) : DataSet.Network.RateLimit

    }

    @Serializable
    data class Cache(
        @YamlComment("Interval to clear cached holder data after write/access (in seconds)")
        @SerialName("cache_expiration")
        override val cacheExpiration: Long = 300,


        @YamlComment("Maximum number of cached holder data by each scope")
        @SerialName("cache_max_size")
        override val cacheMaxSize: Long = 1000,
    ) : DataSet.Cache


    @Serializable
    data class Ping(
        @YamlComment("The interval in seconds between pings")
        override val interval: Int,

        @YamlComment("The extra interval in milliseconds to wait before considering a node dead")
        @SerialName("extra_interval")
        override val extraInterval: Int,
    ) : DataSet.Ping

    @Serializable
    data class Shutdown(
        @SerialName("grace_period")
        override val gracePeriod: Long = 5000L,


        override val timeout: Long = 10000L,


        @SerialName("unit")
        private val _unit: String = "MILLISECONDS"
    ) : DataSet.Shutdown {

        override val unit: TimeUnit
            get() = TimeUnit.valueOf(_unit.uppercase())
    }
}