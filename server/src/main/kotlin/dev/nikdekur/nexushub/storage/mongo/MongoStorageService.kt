/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.storage.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import dev.nikdekur.ndkore.ext.info
import dev.nikdekur.ndkore.service.inject
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.dataset.DataSetService
import dev.nikdekur.nexushub.dataset.get
import dev.nikdekur.nexushub.storage.StorageService
import dev.nikdekur.nexushub.storage.StorageTable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.slf4j.LoggerFactory
import java.io.File


class MongoStorageService(
    override val app: NexusHubServer
) : StorageService {

    val logger = LoggerFactory.getLogger(javaClass)

    val dataSetService: DataSetService by inject()

    lateinit var client: MongoClient
    lateinit var database: MongoDatabase

    override lateinit var scope: CoroutineScope

    override fun onEnable() {
        logger.info { "Initializing Database" }

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val configFile = File("mongo.yml")
        require(configFile.exists()) { "Config file not found" }

        val config = dataSetService.get<MongoDataSet>("mongo")
            ?: error("Config for Mongo not found")

        val serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build()

        val pojoCodecRegistry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        )

        val connectionString = ConnectionString(config.url)

        val mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .codecRegistry(pojoCodecRegistry)
            .serverApi(serverApi)
            .build()


        // Create a new client and connect to the server
        client = MongoClient.create(mongoClientSettings)
        database = client.getDatabase("nexushub")

        logger.info { "Database initialized" }
    }

    override fun onDisable() {
        logger.info { "Disconnecting from Database" }
        scope.cancel()
        client.close()
    }


    override fun getAllTables(): Flow<String> {
        return database.listCollectionNames()
    }

    override fun <T : Any> getTable(name: String, clazz: Class<T>): StorageTable<T> {
        return MongoStorageTable(database.getCollection(name, clazz))
    }
}

inline fun <reified T : Any> StorageService.getTable(name: String): StorageTable<T> {
    return getTable(name, T::class.java)
}