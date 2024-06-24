package org.ndk.nexushub.network.dsl

import org.ndk.nexushub.packet.Packet

data class PacketReaction<R>(
    val receiveHandles: MutableMap<Class<out Packet>?, ReceiveHandler<Packet, R>>,
    val timeouts: MutableMap<Long, TimeoutHandler<R>>,
    val onException: ExceptionHandler<R>?
) {



    class Builder<R> {
        val receiveHandles = HashMap<Class<out Packet>?, ReceiveHandler<out Packet, R>>()
        val timeouts = HashMap<Long, TimeoutHandler<R>>()
        var onException: ExceptionHandler<R>? = null


        @PacketReactionDSL
        @Suppress("UNCHECKED_CAST")
        fun <T : Packet> receive(clazz: Class<out T>, block: ReceiveHandler<T, R>) {
            receiveHandles[clazz] = block as ReceiveHandler<Packet, R>
        }

        @PacketReactionDSL
        inline fun <reified T : Packet> receive(noinline block: ReceiveHandler<T, R>) = receive(T::class.java, block)

        @PacketReactionDSL
        @JvmName("receiveDefault")
        fun receive(block: ReceiveHandler<Packet, R>) {
            receiveHandles[null] = block
        }

        @PacketReactionDSL
        fun timeout(timeout: Long, block: TimeoutHandler<R>) {
            timeouts[timeout] = block
        }

        @PacketReactionDSL
        fun throwOnTimeout(timeout: Long) {
            timeouts[timeout] = {
                throw Exception("Timeout while waiting for packet")
            }
        }

        @PacketReactionDSL
        fun exception(block: ExceptionHandler<R>?) {
            onException = block
        }

        fun build(): PacketReaction<R> {
            return PacketReaction(
                receiveHandles,
                timeouts,
                onException
            )
        }
    }

}