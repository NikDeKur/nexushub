package org.ndk.nexushub

import kotlinx.coroutines.*
import org.ndk.global.scheduler.impl.CoroutineScheduler
import org.ndk.klib.addShutdownHook
import org.ndk.nexushub.client.NexusHub
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

    fun init() {
        // Task will freeze the thread executing in
        scope.launch {
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
                    val data = hub.statService.request(
                        "miragebot_members_1168627596813140019_6",
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


