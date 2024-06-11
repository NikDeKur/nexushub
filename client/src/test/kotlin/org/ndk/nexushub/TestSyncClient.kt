package org.ndk.nexushub

import kotlinx.coroutines.*
import org.ndk.global.scheduler.impl.CoroutineScheduler
import org.ndk.klib.addShutdownHook
import org.ndk.nexushub.client.NexusHub
import org.ndk.nexushub.client.service.AbstractNexusService
import org.ndk.nexushub.client.sesion.SessionImpl
import kotlin.system.exitProcess

object TestSyncClient {
    val scope = CoroutineScheduler(
        CoroutineScope(Dispatchers.Default + SupervisorJob())
    )

    val hub: NexusHub = NexusHub.build {
        node = "TestSyncClient"
        host = "localhost"
        port = 8085
        username = "NikDeKur"
        password = "Nikita08"
        onReady {
            initChat()
        }
    }

    val service = object : AbstractNexusService<String, SessionImpl.FinalSession<String>>(hub) {

        override val scope: String = "miragebot_members"

        override fun createSession(holder: String) = SessionImpl.FinalSession(this, holder)

        override fun getId(holder: String) = holder
        override fun getName(holder: String) = "name-$holder"
    }

    fun init() {
        // Task will freeze the thread executing in
        scope.launch {
            hub.addService(service)
            hub.start()
        }

        addShutdownHook {
            runBlocking {
                println("Stopping...")
                hub.stop()
                println("Stopped")
            }
        }

        Thread.sleep(Long.MAX_VALUE)
    }

    fun initChat() {
        scope.launch {
            while (true) {
                inputLoop()
            }
        }
    }

    suspend fun inputLoop() {
        println("Actions:")
        println("0. Exit")
        println("1. Get data")
        println("2. Set data")
        println("3. Load data")
        println("4. Save data")

        while (true) {
            val input = readln().toIntOrNull() ?: return

            when (input) {
                0 -> {
                    exitProcess(0)
                }

                1 -> {
                    val data = service.getActualData(
                        "852966542986838056"
                    )
                    println("Data: $data")
                }
            }
        }
    }
}


fun main() {
    TestSyncClient.init()
}


