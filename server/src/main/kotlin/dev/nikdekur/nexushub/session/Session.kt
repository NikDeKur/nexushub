package dev.nikdekur.nexushub.session

import dev.nikdekur.nexushub.node.ClientNode
import dev.nikdekur.nexushub.scope.Scope

data class Session(
    val node: ClientNode,
    val scope: Scope,
    val holderId: String,
)