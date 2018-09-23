/*
 * Copyright 2018 Kaidan Gustave
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("CommandUtil")
@file:Suppress("unused", "NOTHING_TO_INLINE")
package xyz.laxus.bot.command

import kotlin.contracts.contract

inline fun commandError(msg: String): Nothing = throw CommandError(msg)

inline fun commandError(msg: () -> String): Nothing = commandError(msg())

inline fun commandErrorIf(condition: Boolean, msg: () -> String) {
    contract { returns() implies !condition }
    if(condition) commandError(msg)
}

inline fun commandErrorUnless(condition: Boolean, msg: () -> String) {
    contract { returns() implies condition }
    if(condition) commandError(msg)
}

inline fun <T> Result<T>.getOrCommandError(msg: () -> String): T {
    return getOrElse { throw CommandError(msg(), it) }
}

inline fun <T: Any> commandErrorIfNull(value: T?, msg: () -> String): T {
    contract { returns() implies (value != null) }
    return value ?: throw CommandError(msg())
}
