/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2024-present "Nik De Kur"
 */

package dev.nikdekur.nexushub.network.dsl


typealias TimeoutHandler<R> = suspend HandlerContext.Timeout<R>.() -> R

typealias ExceptionHandler<R> = suspend HandlerContext.Exception<R>.() -> R

typealias ReceiveHandler<P, R> = suspend HandlerContext.Receive<out P, R>.() -> R
