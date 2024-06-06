package org.ndk.nexushub.network.dsl

import org.ndk.nexushub.network.packet.Packet

data class PacketReaction<R>(
    val timeout: Long?,
    val receiveHandles: HashMap<Class<out Packet>?, ReceiveHandler<out Packet, R>>,
    val onTimeout: TimeoutHandler<R>?,
    val onException: ExceptionHandler<R>?
) {



    class Builder<R> {
        private var timeout: Long? = null
        var receiveHandles = HashMap<Class<out Packet>?, ReceiveHandler<out Packet, R>>()
        private var onTimeout: TimeoutHandler<R>? = null
        private var onException: ExceptionHandler<R>? = null


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
            this.timeout = timeout
            onTimeout = block
        }

        @PacketReactionDSL
        fun exception(block: ExceptionHandler<R>?) {
            onException = block
        }

        fun build(): PacketReaction<R> {
            return PacketReaction(
                timeout,
                receiveHandles,
                onTimeout,
                onException
            )
        }
    }

}