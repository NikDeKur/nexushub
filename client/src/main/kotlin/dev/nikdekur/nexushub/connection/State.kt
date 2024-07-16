package dev.nikdekur.nexushub.connection

sealed class State(val retry: Boolean) {
    object Stopped : State(false)
    class Running(retry: Boolean) : State(retry)
    object Detached : State(false)
}
