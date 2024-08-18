/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset.config

import com.charleskorn.kaml.Yaml
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.dataset.DataSet
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.service.NexusHubService
import kotlinx.serialization.decodeFromString
import java.io.File

class ConfigDataSetService(
    override val app: NexusHubServer,
    val configFile: File
) : NexusHubService, DataSetService {

    var config: DataSetConfig? = null

    override fun onLoad() {
        config = configFile.readText().let {
            Yaml.default.decodeFromString<DataSetConfig>(it)
        }
    }

    override fun onUnload() {
        config = null
    }


    override fun getDataSet(): DataSet {
        return config ?: error("Service not loaded or config load failed")
    }
}