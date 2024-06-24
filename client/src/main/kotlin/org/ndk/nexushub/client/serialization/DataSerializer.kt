package org.ndk.nexushub.client.serialization

import org.ndk.nexushub.client.sesion.Session

interface DataSerializer<H, S> {
    fun serialize(session: Session<H, S>, data: S): String
    fun deserialize(session: Session<H, S>, dataJson: String): S
}