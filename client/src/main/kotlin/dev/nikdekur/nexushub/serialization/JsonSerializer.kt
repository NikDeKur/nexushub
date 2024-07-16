package dev.nikdekur.nexushub.serialization

import dev.nikdekur.nexushub.sesion.Session
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer

open class JsonSerializer<H, S : Any>(
    val format: StringFormat,
    val serializer: KSerializer<S>
) : DataSerializer<H, S> {

    @OptIn(InternalSerializationApi::class)
    override fun serialize(session: Session<H, S>, data: S): String {
        @Suppress("kotlin:S6530") // Serializer is always of type KSerializer<S>
        val serializer = data::class.serializer() as KSerializer<S>
        return format.encodeToString(serializer, data)
    }

    override fun deserialize(session: Session<H, S>, dataJson: String): S {
        return format.decodeFromString(serializer, dataJson)
    }

    override fun isDefault(session: Session<H, S>, data: S): Boolean {
        return data == deserialize(session, "{}")
    }
}