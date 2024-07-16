package dev.nikdekur.nexushub.serialization

import dev.nikdekur.nexushub.sesion.Session


interface DataSerializer<H, S> {
    fun serialize(session: Session<H, S>, data: S): String
    fun deserialize(session: Session<H, S>, dataJson: String): S

    fun isDefault(session: Session<H, S>, data: S): Boolean
}