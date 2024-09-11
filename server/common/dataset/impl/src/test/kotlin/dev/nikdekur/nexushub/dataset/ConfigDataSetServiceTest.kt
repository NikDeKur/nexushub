/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset

import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.dataset.config.ConfigDataSetService
import dev.nikdekur.nexushub.lightWeightNexusHubServer
import java.io.File

class ConfigDataSetServiceTest : DataSetServiceTest {

    val config = """
                key1: value1
                key2: 2
                key3: true
                key4: 4.0
                key5:
                  key1: value1
                  key2: 2
                  key3: true
                  key4: 4.0
                key6:
                  structs:
                    - key1: value1
                      key2: 2
                      key3: true
                      key4: 4.0
                      
                    - key1: value2
                      key2: 3
                      key3: false
                      key4: 5.0
                """.trimIndent()

    val server = lightWeightNexusHubServer {
        service(::ConfigDataSetService, DataSetService::class)

        environment {
            val file = File.createTempFile("config", ".yml")
            file.writeText(config)
            value("config", file.absolutePath)
        }
    }


    override val dataset: DataSetService by server.inject()
}