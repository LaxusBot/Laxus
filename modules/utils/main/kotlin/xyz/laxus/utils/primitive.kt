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
@file:JvmName("PrimitiveUtil")
@file:Suppress("Unused", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
package xyz.laxus.utils

import kotlin.random.Random
import kotlin.ranges.random as kotlinRandomRange
import java.lang.Integer as JvmInt
import java.lang.Long as JvmLong
import java.lang.Double as JvmDouble
import java.lang.Float as JvmFloat

val Int.name: String? get() = Character.getName(this)
fun Int.toChars(): CharArray = Character.toChars(this)

fun Int.toBinaryString(): String = JvmInt.toBinaryString(this)
fun Int.toHexString(): String = JvmInt.toHexString(this)
fun Int.toOctalString(): String = JvmInt.toOctalString(this)
fun Int.toUnsignedString(): String = JvmInt.toUnsignedString(this)

fun Long.toBinaryString(): String = JvmLong.toBinaryString(this)
fun Long.toHexString(): String = JvmLong.toHexString(this)
fun Long.toOctalString(): String = JvmLong.toOctalString(this)
fun Long.toUnsignedString(): String = JvmLong.toUnsignedString(this)

fun Double.toHexString(): String = JvmDouble.toHexString(this)
fun Float.toHexString(): String = JvmFloat.toHexString(this)

fun emptyByteArray() = ByteArray(0)
fun emptyShortArray() = ShortArray(0)
fun emptyIntArray() = IntArray(0)
fun emptyLongArray() = LongArray(0)

/**
 * Generates a pseudo-random [Double] in the range [[from], [until]).
 *
 * This uses the following relatively common lower-upper-bounded equation:
 *
 * ```
 * from + (r * (until - from))
 * ```
 *
 * where `r` is a random decimal in the range [0.0, 1.0).
 *
 * @param from  The minimum [integer][Int] that can be generated (inclusive).
 * @param until The maximum [integer][Int] that can be generated (exclusive).
 *
 * @return A pseudo-random [Double] in the range [[from], [until]).
 *
 * @throws IllegalArgumentException If [until] <= [from]
 *
 * @see Random.nextDouble
 */
fun Random.nextDouble(from: Int = 0, until: Int = 1): Double {
    require(from < until) { "Invalid range: $from is not less than $until!" }

    return from + (nextDouble() * (until - from))
}


// DEPRECATED

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
 * @see Random.nextDouble
 */
@Deprecated(
    message = "Not replaced, but extension created to be more inline with semantics of other functions",
    replaceWith = ReplaceWith(
        expression = "Random.Companion.nextDouble(from = min, until = max)",
        imports = ["kotlin.random.Random", "xyz.laxus.utils.nextDouble"]
    )
)
fun random(min: Int = 0, max: Int = 1): Double = Random.nextDouble(min, max)

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
 * @see Random.nextDouble
 */
@Deprecated(
    message = "Now included in kotlin-stdlib as of 1.3 (in Random utility class)",
    replaceWith = ReplaceWith(
        expression = "Random.Companion.nextInt(until = max)",
        imports = ["kotlin.random.Random"]
    )
)
fun randomInt(max: Int): Int = Random.nextInt(until = max)

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
 * @see Random.nextDouble
 */
@Deprecated(
    message = "Now included in kotlin-stdlib as of 1.3 (in Random utility class)",
    replaceWith = ReplaceWith(
        expression = "Random.Companion.nextInt(from = min, until = max)",
        imports = ["kotlin.random.Random"]
    )
)
fun randomInt(min: Int, max: Int): Int = Random.nextInt(from = min, until = max)

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
 * @see Random.nextDouble
 */
@Deprecated(
    message = "Now included in kotlin-stdlib as of 1.3",
    replaceWith = ReplaceWith(
        expression = "random()",
        imports = ["kotlin.ranges.random"]
    )
)
fun IntRange.random(): Int = kotlinRandomRange()
