/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.boot

import dev.nikdekur.ndkore.ext.printAverageExecTime
import dev.nikdekur.nexushub.TestNexusHubServer
import dev.nikdekur.nexushub.dataset.DataSet
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.test.TestDataSetService
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.runtime.RuntimeStorageService
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class SimpleTest {

    @Test
    fun firstTest() {
        printAverageExecTime(1) {
            val environment = object : Environment {
                override fun getValue(key: String): String? {
                    return null
                }

                override fun requestValue(key: String, description: String): String? {
                    if (key == "root_password") {
                        return "root"
                    }
                    return null
                }
            }

            val dataset: (TestNexusHubServer) -> DataSetService = {
                TestDataSetService(it,
                    object : DataSet {
                        override val network = object : DataSet.Network {
                            override val port = 8080
                            override val ssl = null
                            override val rateLimit = object : DataSet.Network.RateLimit {
                                override val maxRequests = 100
                                override val timeWindow = 60
                            }
                        }

                        override val cache = object : DataSet.Cache {
                            override val cacheExpiration = 300L
                            override val cacheMaxSize = 100L

                        }
                        override val ping = object : DataSet.Ping {
                            override val interval = 60
                            override val extraInterval = 10
                        }
                        override val shutdown = object : DataSet.Shutdown {
                            override val gracePeriod = 3L
                            override val timeout = 3L
                            override val unit = TimeUnit.SECONDS
                        }
                    }
                )
            }

            val storage: (TestNexusHubServer) -> StorageService = {
                RuntimeStorageService(it)
            }

            val server = TestNexusHubServer(environment, dataset, storage)
            server.start()
        }
    }
}