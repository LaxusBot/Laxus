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
@file:JvmName("PrimitivesKt")
package xyz.laxus.util

import kotlin.math.roundToInt

val Int.name: String? get() = Character.getName(this)
val Long.length: Int get() = "$this".length

fun Int.toChars(): CharArray = Character.toChars(this)
fun Int.toBinaryString(): String = Integer.toBinaryString(this)
fun Int.toHexString(): String = Integer.toHexString(this)
fun Int.toOctalString(): String = Integer.toOctalString(this)
fun Int.toUnsignedString(): String = Integer.toUnsignedString(this)

fun emptyByteArray() = ByteArray(0)
fun emptyShortArray() = ShortArray(0)
fun emptyIntArray() = IntArray(0)
fun emptyLongArray() = LongArray(0)

fun arrayOf(vararg bytes: Byte) = bytes
fun arrayOf(vararg shorts: Short) = shorts
fun arrayOf(vararg ints: Int) = ints
fun arrayOf(vararg longs: Long) = longs

/**
 * Generates a pseudo-random [Double] in the range [[min], [max]).
 *
 * This uses the following relatively common lower-upper-bounded equation:
 *
 * ```
 * min + (r * (max - min))
 * ```
 *
 * where `r` is a random decimal in the range [0.0, 1.0).
 *
 * @param min The minimum [integer][Int] that can be generated (inclusive).
 * @param max The maximum [integer][Int] that can be generated (exclusive).
 *
 * @return A pseudo-random [Double] in the range [[min], [max]).
 *
 * @see Math.random
 */
fun random(min: Int = 0, max: Int = 1): Double {
    require(min < max) { "Invalid range: $min is not less than $max!" }

    return min + (Math.random() * (max - min))
}

/**
 * Generates a pseudo-random [Int] in the range [0, [max]).
 *
 * @param max The maximum [integer][Int] that can be generated (exclusive).
 *
 * @return A pseudo-random [Int] in the range [0, [max]).
 *
 * @see random
 * @see Math.random
 */
fun randomInt(max: Int): Int = randomInt(0, max)

/**
 * Generates a pseudo-random [Int] in the range [[min], [max]).
 *
 * @param min The minimum [integer][Int] that can be generated (inclusive).
 * @param max The maximum [integer][Int] that can be generated (exclusive).
 *
 * @return A pseudo-random [Int] in the range [[min], [max]).
 *
 * @see random
 * @see Math.random
 */
fun randomInt(min: Int, max: Int): Int = random(min, max).roundToInt()

/**
 * Generates a pseudo-random [Int] in this range.
 *
 * @receiver An [IntRange] to generate from.
 *
 * @return A pseudo-random [Int] in this range.
 *
 * @see random
 * @see randomInt
 * @see Math.random
 */
fun IntRange.random(): Int = randomInt(min = first, max = last)
