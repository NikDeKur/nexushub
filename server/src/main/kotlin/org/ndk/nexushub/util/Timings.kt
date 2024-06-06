package org.ndk.nexushub.util

inline fun <T> logTiming(name: String, block: () -> T): T {
    val start = System.currentTimeMillis()
    val result = block()
    val end = System.currentTimeMillis()
    println("$name took ${end - start}ms")
    return result
}