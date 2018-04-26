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

@Deprecated(
    message = "Kotlin stdlib already contains a suitable function.",
    replaceWith = ReplaceWith(
        expression = "byteArrayOf(Array<out Byte>)",
        imports = ["kotlin.byteArrayOf"]
    ),
    level = DeprecationLevel.HIDDEN
)
fun arrayOf(vararg bytes: Byte) = byteArrayOf(*bytes)

@Deprecated(
    message = "Kotlin stdlib already contains a suitable function.",
    replaceWith = ReplaceWith(
        expression = "shortArrayOf(Array<out Short>)",
        imports = ["kotlin.shortArrayOf"]
    ),
    level = DeprecationLevel.HIDDEN
)
fun arrayOf(vararg shorts: Short) = shortArrayOf(*shorts)

@Deprecated(
    message = "Kotlin stdlib already contains a suitable function.",
    replaceWith = ReplaceWith(
        expression = "intArrayOf(Array<out Int>)",
        imports = ["kotlin.intArrayOf"]
    ),
    level = DeprecationLevel.HIDDEN
)
fun arrayOf(vararg ints: Int) = intArrayOf(*ints)

@Deprecated(
    message = "Kotlin stdlib already contains a suitable function.",
    replaceWith = ReplaceWith(
        expression = "longArrayOf(Array<out Long>)",
        imports = ["kotlin.longArrayOf"]
    ),
    level = DeprecationLevel.HIDDEN
)
fun arrayOf(vararg longs: Long) = longArrayOf(*longs)

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
 * @throws IllegalArgumentException If [max] <= [min]
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
 * @throws IllegalArgumentException If [max] <= 0
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
 * @throws IllegalArgumentException If [max] <= [min]
 *
 * @see random
 * @see Math.random
 */
fun randomInt(min: Int, max: Int): Int = random(min, max).roundToInt()

/**
 * Generates a pseudo-random [Int] in this range
 *
 * The receiver should be interpreted as start inclusive, end exclusive.
 *
 * @receiver An [IntRange] to generate from.
 *
 * @return A pseudo-random [Int] in this range.
 *
 * @throws IllegalArgumentException If [first][IntRange.first] <= [last][IntRange.last]
 *
 * @see random
 * @see randomInt
 * @see Math.random
 */
fun IntRange.random(): Int = randomInt(min = first, max = last)
