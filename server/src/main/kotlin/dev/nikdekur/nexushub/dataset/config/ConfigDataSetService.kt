/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.dataset.config

import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.map.MapDataSetService
import dev.nikdekur.nexushub.service.NexusHubService
import net.mamoe.yamlkt.Yaml
import java.io.File
import kotlin.reflect.KClass

class ConfigDataSetService(
    override val app: NexusHubServer
) : NexusHubService(), DataSetService {

    private var _delegate: MapDataSetService? = null
    val delegate
        get() = _delegate ?: error("Config not loaded!")

    override fun onEnable() {
        val configPath = app.environment.getValue("config") ?: "config.yml"
        val configFile = File(configPath)
        require(configFile.exists()) { "Config file not found!" }

        @Suppress("UNCHECKED_CAST")
        _delegate = MapDataSetService(
            app,
            Yaml.decodeMapFromString(configFile.readText()) as Map<String, Any>
        ).also { it.onEnable() }
    }

    override fun onDisable() {
        _delegate?.onDisable()
        // Don't nullify delegate here, as might be necessary for shutdown tasks
    }


    override fun <T : Any> get(key: String, clazz: KClass<T>) = delegate.get<T>(key, clazz)
    override fun getSection(key: String) = delegate.getSection(key)
}