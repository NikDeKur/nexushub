package org.ndk.nexushub.network.dsl


typealias TimeoutHandler<R> = suspend HandlerContext.Timeout<R>.() -> R

typealias ExceptionHandler<R> = suspend HandlerContext.Exception<R>.() -> R

typealias ReceiveHandler<P, R> = suspend HandlerContext.Receive<out P, R>.() -> R
