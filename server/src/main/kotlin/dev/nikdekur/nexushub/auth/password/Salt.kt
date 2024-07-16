package dev.nikdekur.nexushub.auth.password

data class Salt(
    val byte: ByteArray
) {

    @OptIn(ExperimentalStdlibApi::class)
    val hex = byte.toHEX()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Salt

        return byte.contentEquals(other.byte)
    }

    override fun hashCode(): Int {
        return byte.contentHashCode()
    }
}
