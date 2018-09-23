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
@file:JvmName("ExceptionUtil")
@file:Suppress("Unused")
package xyz.laxus.utils

import kotlin.contracts.InvocationKind.*
import kotlin.contracts.contract

// UnsupportedOperationException

/**
 * Throws an [UnsupportedOperationException] with
 * the provided [msg].
 *
 * @param msg The message.
 *
 * @throws UnsupportedOperationException with the provided [msg].
 */
inline fun unsupported(msg: () -> String): Nothing {
    contract { callsInPlace(msg, EXACTLY_ONCE) }
    throw UnsupportedOperationException(msg())
}

/**
 * Throws an [UnsupportedOperationException] with
 * the provided [msg] if the [condition] is `true`.
 *
 * @param condition The condition.
 * @param msg The message.
 *
 * @throws UnsupportedOperationException If the [condition] is `true`.
 */
inline fun doNotSupport(condition: Boolean, msg: () -> String = { "This condition is unsupported!" }) {
    contract {
        callsInPlace(msg, AT_MOST_ONCE)
        returns() implies !condition
    }
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
inline fun onlySupport(condition: Boolean, msg: () -> String = { "Only this condition is supported!" }) {
    contract {
        callsInPlace(msg, AT_MOST_ONCE)
        returns() implies condition
    }
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
inline fun indexOutOfBounds(msg: () -> String): Nothing {
    contract { callsInPlace(msg, EXACTLY_ONCE) }
    throw IndexOutOfBoundsException(msg())
}

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
inline fun checkInBounds(from: Int, to: Int, index: () -> Int) {
    contract { callsInPlace(index, EXACTLY_ONCE) }
    checkInBounds(from, to, index())
}

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
inline fun checkInBounds(from: Long, to: Long, index: () -> Long) {
    contract { callsInPlace(index, EXACTLY_ONCE) }
    checkInBounds(from, to, index())
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
    contract { callsInPlace(block, EXACTLY_ONCE) }
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
@Deprecated(
    message = "Use new SuccessOrFailure API",
    replaceWith = ReplaceWith(
        expression = "runCatching(block).getOrDefault(default)",
        imports = ["kotlin.runCatching", "kotlin.getOrDefault"]
    )
)
inline fun <reified T> ignored(default: T, block: () -> T): T = runCatching(block).getOrDefault(default)
