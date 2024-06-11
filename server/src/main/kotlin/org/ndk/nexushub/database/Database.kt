package org.ndk.nexushub.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.ServerApi
import com.mongodb.ServerApiVersion
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.ndk.global.scheduler.Scheduler
import org.ndk.global.scheduler.impl.CoroutineScheduler
import org.ndk.nexushub.NexusHub
import org.ndk.nexushub.NexusHub.logger


object Database {

    lateinit var client: MongoClient
    lateinit var database: MongoDatabase

    lateinit var scheduler: Scheduler

    fun init() {

        val user = NexusHub.config.database.username
        val password = NexusHub.config.database.password

        scheduler = CoroutineScheduler(NexusHub.blockingScope)

        val connectionString = "mongodb+srv://$user:$password@cluster.wtky2pf.mongodb.net/?retryWrites=true&w=majority&appName=cluster"

        val serverApi = ServerApi.builder()
            .version(ServerApiVersion.V1)
            .build()

        val pojoCodecRegistry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(PojoCodecProvider.builder().automatic(true).build())
        )

        val mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(connectionString))
            .codecRegistry(pojoCodecRegistry)
            .serverApi(serverApi)
            .build()


        // Create a new client and connect to the server
        client = MongoClient.create(mongoClientSettings)
        database = client.getDatabase("nexushub")

        logger.info("Connected to the database")
    }
}