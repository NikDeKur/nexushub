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
import dev.nikdekur.nexushub.auth.account.AccountsService
import dev.nikdekur.nexushub.auth.account.AccountsServiceImpl
import dev.nikdekur.nexushub.config.NexusHubServerConfig
import dev.nikdekur.nexushub.database.Database
import dev.nikdekur.nexushub.database.account.AccountDAO
import dev.nikdekur.nexushub.database.account.MongoAccountsTable
import dev.nikdekur.nexushub.database.mongo.scope.MongoScopesTable
import dev.nikdekur.nexushub.database.mongo.scope.ScopeDAO
import dev.nikdekur.nexushub.koin.NexusHubComponent
import dev.nikdekur.nexushub.koin.loadModule
import dev.nikdekur.nexushub.scope.MongoScopesService
import dev.nikdekur.nexushub.scope.ScopesService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.koin.core.component.inject
import org.koin.dsl.bind
import org.slf4j.LoggerFactory


class MongoDatabaseImpl : Database, NexusHubComponent {

    val logger = LoggerFactory.getLogger(javaClass)

    val config: NexusHubServerConfig by inject()

    val client: MongoClient
    val database: MongoDatabase

    lateinit var scopesService: MongoScopesService

    init {
        logger.info { "Initializing Database" }

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

        runBlocking {
            val scopesTable = MongoScopesTable(
                database.ensureCollectionExists<ScopeDAO>("scopes") {
                    val indexOptions = indexOptions {
                        unique(true)
                    }

                    createIndex(Document("name", 1), indexOptions)
                }
            )

            scopesService = MongoScopesService(this@MongoDatabaseImpl, scopesTable)

            val accountsTable = MongoAccountsTable(
                database.ensureCollectionExists<AccountDAO>("accounts") {
                    val indexOptions = indexOptions {
                        unique(true)
                    }

                    createIndex(Document("login", 1), indexOptions)
                }
            )

            val accountsService = AccountsServiceImpl(accountsTable)

            logger.info { "Database initialized" }

            loadModule {
                single { scopesService } bind ScopesService::class
                single { accountsService } bind AccountsService::class
            }
        }
    }

    override fun getAllCollectionsNames(): Flow<String> {
        return database.listCollectionNames()
    }

}