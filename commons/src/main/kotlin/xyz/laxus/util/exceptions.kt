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
@file:Suppress("Unused")
package xyz.laxus.util

import xyz.laxus.annotation.Experimental
import kotlin.reflect.KVisibility
import kotlin.reflect.full.starProjectedType

/**
 * Throws an [UnsupportedOperationException] with
 * the provided [msg].
 *
 * @param msg The message.
 */
inline fun unsupported(msg: () -> String): Nothing = throw UnsupportedOperationException(msg())

/**
 * Throws an [UnsupportedOperationException] with
 * the provided [msg] if the [condition] is `true`.
 *
 * @param condition The condition.
 * @param msg The message.
 */
inline fun doNotSupport(condition: Boolean, msg: () -> String) {
    if(condition) {
        unsupported(msg)
    }
}

/**
 * Throws an [UnsupportedOperationException] with
 * the provided [msg] if the [condition] is `false`.
 *
 * @param condition The condition.
 * @param msg The message.
 */
inline fun onlySupport(condition: Boolean, msg: () -> String) {
    if(!condition) {
        unsupported(msg)
    }
}

/**
 * Throws an exception of the specified [type][T] with
 * the provided [msg].
 *
 * This functionality works for types that have a public
 * constructor with a single string parameter as their
 * arguments.
 *
 * @param T The type of exception to throw.
 * @param msg The message.
 */
@Experimental(
    "This entity uses reflections that could potentially " +
    "create runtime errors from within inlined function " +
    "code, which is highly unappealing behavior. Further " +
    "testing of the effects of throwing exceptions is required " +
    "before this can be determined as appropriate."
)
inline fun <reified T: Throwable> raise(msg: () -> String): Nothing {
    val constructor = T::class.constructors.first {
        if(it.visibility != KVisibility.PUBLIC)
            return@first false
        val params = it.parameters
        if(params.isEmpty() || params.size > 1)
            return@first false
        return@first params[0].type == String::class.starProjectedType
    }

    throw constructor.call(msg())
}

/**
 * Runs the [block] of code, ignoring any and
 * all [exceptions][Throwable] thrown during
 * execution.
 *
 * @param block The block to run.
 */
inline fun ignored(block: () -> Unit) {
    try { block() } catch(t: Throwable) {}
}

/**
 * Runs the [block] of code, ignoring any and all [exceptions][Throwable] thrown
 * during execution.
 *
 * If an exception is thrown, this will return the [default] value, otherwise it
 * return the resulting value of the [block].
 *
 * @param default The default value returned if an exception occurs in the [block].
 * @param block The block to run.
 */
inline fun <reified T> ignored(default: T, block: () -> T): T {
    return try { block() } catch(t: Throwable) { default }
}

@Deprecated(
    message = "Throwing exceptions within inline functions is unadvised, this behavior should " +
              "be replicated with a normal try-catch block instead.",
    replaceWith = ReplaceWith(expression = "try { block() } catch(ignored: T) {}"),
    level = DeprecationLevel.HIDDEN
)
inline fun <reified T: Throwable> ignore(block: () -> Unit) {
    try {
        block()
    } catch(t: Throwable) {
        if(t is T) return
        throw t
    }
}
