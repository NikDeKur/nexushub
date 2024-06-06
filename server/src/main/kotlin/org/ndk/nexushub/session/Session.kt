package org.ndk.nexushub.session

import org.ndk.nexushub.node.ClientNode
import org.ndk.nexushub.scope.Scope

data class Session(
    val node: ClientNode,
    val scope: Scope,
    val holderId: String,
)