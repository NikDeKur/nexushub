package org.ndk.nexushub

import kotlinx.coroutines.*
import org.ndk.global.scheduler.impl.CoroutineScheduler
import org.ndk.klib.input
import org.ndk.klib.parallel
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.service.AbstractNexusService
import org.ndk.nexushub.client.sesion.SessionImpl
import java.util.concurrent.ConcurrentHashMap

object TestConsoleClient {
    val scope = CoroutineScheduler(
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    )

    val hub: NexusHub = NexusHub.build {
        node = "TestConsoleClient"
        host = "localhost"
        port = 8085
        username = "NikDeKur"
        password = "Nikita08"
        onReady {
            initChat()

            scope.runTaskTimer(1000 * 60, ::saveTask)
        }
    }

    suspend fun saveTask() {
        val sessions = service.sessionsCache.asMap().values
        hub.blockingScope.parallel(service.saveParallelism, sessions) {
            println("Saving data for ${it.holder}...")
            it.stop()
        }.awaitAll()
    }

    val service = object : AbstractNexusService<String, SessionImpl.FinalSession<String>>(hub) {

        override val scope: String = "test-service"

        override fun createSession(holder: String): SessionImpl.FinalSession<String> {
            return SessionImpl.FinalSession(this, holder)
        }

        override fun getId(holder: String) = "id-$holder"
        override fun getName(holder: String) = "name-$holder"
    }

    fun init() {
        // Task will freeze the thread executing in
        runBlocking {
            println("Booting...")
            hub.addService(service)
            hub.start()
            println("Shutdown...")
        }
    }

    fun initChat() {
        scope.launch {
            inputLoop()
        }
    }

    const val HOLDER = "NikGdeSru"

    suspend fun inputLoop() {
        println("Actions:")
        println("0. Exit")
        println("1. Get data")
        println("2. Set data")
        println("3. Load data")
        println("4. Save data")

        while (hub.isRunning) {
            val input = readln().toIntOrNull() ?: continue

            when (input) {
                0 -> {
                    println("Stopping...")
                    hub.stop()
                    println("Stopped")
                }

                1 -> {
                    val session = service.getSession(HOLDER)
                    println("Data: ${session.data}")
                }

                2 -> {
                    val session = service.getSession(HOLDER)
                    val key = input("Enter key: ")
                    val value = input("Enter value: ")
                    session[key] = value
                    println("Successfully set data")
                }

                3 -> {
                    println("Loading data...")
                    val session = service.getSession(HOLDER)
                    session.loadData()
                    println("Data: ${session.data}")
                }

                4 -> {
                    println("Saving data...")
                    val session = service.getSession(HOLDER)
                    session.saveData()
                    println("Data saved")
                }
                5 -> {
                    val session = service.getSession(HOLDER)
                    val someInnerMap: MutableMap<String, String> = session.data.computeIfAbsent("someInnerMap") { ConcurrentHashMap<String, String>() } as MutableMap<String, String>
                    someInnerMap["someKey"] = "someValue"
                    println("Data: ${session.data}")
                }
            }
        }
    }
}


fun main() {
    TestConsoleClient.init()
}


