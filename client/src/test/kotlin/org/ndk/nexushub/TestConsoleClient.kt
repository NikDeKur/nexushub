package org.ndk.nexushub

import kotlinx.coroutines.*
import org.ndk.global.scheduler.impl.CoroutineScheduler
import org.ndk.klib.input
import org.ndk.klib.parallel
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.service.AbstractNexusService
import org.ndk.nexushub.client.sesion.SessionImpl

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
        val sessions = members.sessionsCache.asMap().values
        hub.blockingScope.parallel(members.saveParallelism, sessions) {
            println("Saving data for ${it.holder}...")
            it.saveData()
        }.awaitAll()
    }

    val members = object : AbstractNexusService<String, SessionImpl.FinalSession<String>>(hub) {

        override val scope: String = "miragebot_members"

        override fun createSession(holder: String) = SessionImpl.FinalSession(this, holder)

        override fun getId(holder: String) = holder
        override fun getName(holder: String) = "name-$holder"
    }

    fun init() {
        // Task will freeze the thread executing in
        runBlocking {
            hub.addService(members)
            try {
                hub.start()
            } catch (e: Exception) {
                println("Failed to start NexusHub!")
                e.printStackTrace()
            }
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
        println("5. Get Top Position")
        println("6. Get Leaderboard")

        while (hub.isRunning) {
            val input = readln().toIntOrNull() ?: continue

            when (input) {
                0 -> {
                    println("Stopping...")
                    hub.stop()
                    println("Stopped")
                }

                1 -> {
                    val session = members.getSession(HOLDER)
                    println("Data: ${session.data}")
                }

                2 -> {
                    val session = members.getSession(HOLDER)
                    val key = input("Enter key: ")
                    val value = input("Enter value: ")
                    session[key] = value
                    println("Successfully set data")
                }

                3 -> {
                    println("Loading data...")
                    val session = members.getSession(HOLDER)
                    session.loadData()
                    println("Data: ${session.data}")
                }

                4 -> {
                    println("Saving data...")
                    val session = members.getSession(HOLDER)
                    session.saveData()
                    println("Data saved")
                }
//                5 -> {
//                    val field
//                    val leaderboard = members.getLeaderboard()
//                }

            }
        }
    }
}


fun main() {
    TestConsoleClient.init()
}


