package org.ndk.nexushub

import kotlinx.coroutines.*
import org.ndk.klib.debug
import org.ndk.klib.printExecTime
import org.ndk.nexushub.NexusHub.logger

fun asyncParallelTest() {

    val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    class Client {
        suspend fun isAlive(): Boolean {
            delay(1500)
            return true
        }
    }

    val clients = Array(100) { Client() }
    scope.launch {
        val results = printExecTime {
            clients.map { client ->
                async { client.isAlive() }
            }.awaitAll()
        }
        logger.debug { "Ping results: $results" }
    }

    runBlocking {
        scope.coroutineContext.job.join()
    }

}

fun main() {
    asyncParallelTest()
}