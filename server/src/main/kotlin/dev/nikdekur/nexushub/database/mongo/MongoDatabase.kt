/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.database.mongo

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import dev.nikdekur.ndkore.ext.info
import dev.nikdekur.nexushub.NexusHubServer
import dev.nikdekur.nexushub.config.NexusHubServerConfig
import dev.nikdekur.nexushub.database.Database
import dev.nikdekur.nexushub.service.NexusHubService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.koin.core.component.inject
import org.slf4j.LoggerFactory


class MongoDatabase(
    override val app: NexusHubServer
) : Database, NexusHubService {

    val logger = LoggerFactory.getLogger(javaClass)

    val config: NexusHubServerConfig by inject()

    lateinit var client: MongoClient
    lateinit var database: MongoDatabase

    override lateinit var scope: CoroutineScope

    override fun onLoad() {
        logger.info { "Initializing Database" }

        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build()

        val pojoCodecRegistry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        )

        val connectionString = ConnectionString(config.database.connection)

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

    override fun onUnload() {
        logger.info { "Disconnecting from Database" }
        scope.cancel()
        client.close()
    }



    override fun getAllCollectionsNames(): Flow<String> {
        return database.listCollectionNames()
    }

}