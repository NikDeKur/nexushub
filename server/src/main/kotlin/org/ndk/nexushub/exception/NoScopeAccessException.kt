package org.ndk.nexushub.exception

class NoScopeAccessException(val scopeId: String) : Exception("No scope access for scopeId: $scopeId")