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
@file:JvmName("ExceptionUtil")
package xyz.laxus.util

import xyz.laxus.annotation.Experimental
import kotlin.reflect.KVisibility
import kotlin.reflect.full.starProjectedType

// UnsupportedOperationException

/**
 * Throws an [UnsupportedOperationException] with
 * the provided [msg].
 *
 * @param msg The message.
 *
 * @throws UnsupportedOperationException with the provided [msg].
 */
inline fun unsupported(msg: () -> String): Nothing = throw UnsupportedOperationException(msg())

/**
 * Throws an [UnsupportedOperationException] with
 * the provided [msg] if the [condition] is `true`.
 *
 * @param condition The condition.
 * @param msg The message.
 *
 * @throws UnsupportedOperationException If the [condition] is `true`.
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
 *
 * @throws UnsupportedOperationException If the [condition] is `false`.
 */
inline fun onlySupport(condition: Boolean, msg: () -> String) {
    if(!condition) {
        unsupported(msg)
    }
}

// IndexOutOfBoundsException

/**
 * Throws an [IndexOutOfBoundsException] with the provided [msg].
 *
 * @param msg The message.
 *
 * @throws IndexOutOfBoundsException with the provided [msg].
 */
inline fun indexOutOfBounds(msg: () -> String): Nothing = throw IndexOutOfBoundsException(msg())

// IndexOutOfBoundsException (End-Inclusive)

/**
 * Throws an [IndexOutOfBoundsException] if the [index] specified does not lie in the [range].
 *
 * @param N The type of [Number].
 * @param R The type of [ClosedRange] (should accept [N] as it's type argument).
 * @param index The index to check.
 * @param range The range to check in.
 *
 * @throws IndexOutOfBoundsException If `index` is not in `[range.first, range.last]`.
 */
fun <N: Number, R: ClosedRange<N>> checkInRange(index: N, range: R) {
    if(index !in range) indexOutOfBounds { "Index $index out of range: [${range.start}, ${range.endInclusive}]" }
}

/**
 * Throws an [IndexOutOfBoundsException] if the [index] specified does not lie in the [range].
 *
 * @param N The type of [Number].
 * @param R The type of [ClosedRange] (should accept [N] as it's type argument).
 * @param range The range to check in.
 * @param index The index to check.
 *
 * @throws IndexOutOfBoundsException If `index` is not in `[range.first, range.last]`.
 */
inline fun <reified N: Number, reified R: ClosedRange<N>> checkInRange(range: R, index: () -> N) {
    checkInRange(index(), range)
}

// IndexOutOfBoundsException (End-Exclusive)

/**
 * Throws an [IndexOutOfBoundsException] if the [index] specified
 * does not lie in the bounds [[from], [to]).
 *
 * @param index The index to check.
 * @param from The left bound (inclusive).
 * @param to The right bound (exclusive).
 *
 * @throws IndexOutOfBoundsException If `index` is not in `[from, to)`.
 */
fun checkInBounds(index: Int, from: Int, to: Int) {
    if(index !in from until to) indexOutOfBounds { "Index $index out of range: [$from, $to)" }
}

/**
 * Throws an [IndexOutOfBoundsException] if the [index] specified
 * does not lie in the bounds [[from], [to]).
 *
 * @param index The index to check.
 * @param from The left bound (inclusive).
 * @param to The right bound (exclusive).
 *
 * @throws IndexOutOfBoundsException If `index` is not in `[from, to)`.
 */
fun checkInBounds(index: Long, from: Long, to: Long) {
    if(index !in from until to) indexOutOfBounds { "Index $index out of range: [$from, $to)" }
}

/**
 * Throws an [IndexOutOfBoundsException] if the [index] specified
 * does not lie in the bounds [0, [array].size).
 *
 * @param index The index to check.
 * @param array The [Array] whose boundaries are checked.
 *
 * @throws IndexOutOfBoundsException If `index` is not in `[from, to)`.
 */
fun checkInBounds(index: Int, array: Array<*>) = checkInBounds(index, 0, array.size)

/**
 * Throws an [IndexOutOfBoundsException] if the [index] specified
 * does not lie in the bounds [0, [collection].size).
 *
 * @param index The index to check.
 * @param collection The [Collection] whose boundaries are checked.
 *
 * @throws IndexOutOfBoundsException If `index` is not in `[from, to)`.
 */
fun checkInBounds(index: Int, collection: Collection<*>) = checkInBounds(index, 0, collection.size)

/**
 * Throws an [IndexOutOfBoundsException] if the [index] specified
 * does not lie in the bounds [[from], [to]).
 *
 * @param from The left bound (inclusive).
 * @param to The right bound (exclusive).
 * @param index The index to check.
 *
 * @throws IndexOutOfBoundsException If `index` is not in `[from, to)`.
 */
inline fun checkInBounds(from: Int, to: Int, index: () -> Int) = checkInBounds(from, to, index())

/**
 * Throws an [IndexOutOfBoundsException] if the [index] specified
 * does not lie in the bounds [[from], [to]).
 *
 * @param from The left bound (inclusive).
 * @param to The right bound (exclusive).
 * @param index The index to check.
 *
 * @throws IndexOutOfBoundsException If `index` is not in `[from, to)`.
 */
inline fun checkInBounds(from: Long, to: Long, index: () -> Long) = checkInBounds(from, to, index())

// Misc

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
@Experimental(details = """
    This entity uses reflections that could potentially
    create runtime errors from within inlined function
    code, which is highly unappealing behavior. Further
    testing of the effects of throwing exceptions is required
    before this can be determined as appropriate.
""")
inline fun <reified T: Throwable> raise(msg: () -> String): Nothing {
    val constructor = T::class.constructors.first {
        // Not publicly visible, skip
        if(it.visibility != KVisibility.PUBLIC) {
            return@first false
        }

        val params = it.parameters
        if(params.isEmpty() || params.size > 1) {
            return@first false
        }
        return@first params[0].type == String::class.starProjectedType
    }

    throw constructor.call(msg())
}

// Exception Safety

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
 * @param T The type to return.
 * @param default The default value returned if an exception occurs in the [block].
 * @param block The block to run.
 *
 * @return Either the result of the [block], or [default] if an exception was thrown.
 */
inline fun <reified T> ignored(default: T, block: () -> T): T {
    return try { block() } catch(t: Throwable) { default }
}

@Deprecated(
    message = "Throwing exceptions within inline functions is unadvised, this " +
              "behavior should be replicated with a normal try-catch block instead.",
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
