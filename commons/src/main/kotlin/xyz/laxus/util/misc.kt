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
@file:JvmName("MiscUtil")
@file:Suppress("Unused", "FunctionName", "NOTHING_TO_INLINE")
package xyz.laxus.util

import java.util.*

/**
 * Returns `null` typed as [T?][T].
 *
 * This is effective when working with certain operator
 * functions that use type arguments and do not have
 * explicit type specification abilities.
 *
 * @param T The type.
 *
 * @return `null` typed as [T?][T].
 */
inline fun <T> nullOf(): T? = null

/**
 * Modifies the receiver based on the provided [block]
 * if [condition] returns `true`, or else returns the
 * receiver unchanged.
 *
 * @receiver The entity to modify or not.
 * @param T The type.
 * @param condition The condition to check.
 * @param block The block to run if the condition is `true`.
 *
 * @return The modified value if the [condition] is `true`, or the receiver unchanged if `false`.
 */
inline fun <reified T> T.modifyIf(condition: (T) -> Boolean, block: (T) -> T): T = modifyIf(condition(this), block)

/**
 * Modifies the receiver based on the provided [block]
 * unless [condition] returns `true`, in which case this
 * returns the receiver unchanged.
 *
 * @receiver The entity to modify or not.
 * @param T The type.
 * @param condition The condition to check.
 * @param block The block to run if the condition is `false`.
 *
 * @return The modified value if the [condition] is `false`, or the receiver unchanged if `true`.
 */
inline fun <reified T> T.modifyUnless(condition: (T) -> Boolean, block: (T) -> T): T = modifyUnless(condition(this), block)

/**
 * Modifies the receiver based on the provided [block]
 * if [condition] is `true`, or else returns the
 * receiver unchanged.
 *
 * @receiver The entity to modify or not.
 * @param T The type.
 * @param condition The condition to check.
 * @param block The block to run if the condition is `true`.
 *
 * @return The modified value if the [condition] is `true`, or the receiver unchanged if `false`.
 */
inline fun <reified T> T.modifyIf(condition: Boolean, block: (T) -> T): T = if(condition) block(this) else this

/**
 * Modifies the receiver based on the provided [block]
 * unless [condition] is `true`, in which case this
 * returns the receiver unchanged.
 *
 * @receiver The entity to modify or not.
 * @param T The type.
 * @param condition The condition to check.
 * @param block The block to run if the condition is `false`.
 *
 * @return The modified value if the [condition] is `false`, or the receiver unchanged if `true`.
 */
inline fun <reified T> T.modifyUnless(condition: Boolean, block: (T) -> T): T = modifyIf(!condition, block)

@Deprecated(
    message = "Deprecated in favor of better named property: titleName.",
    replaceWith = ReplaceWith(
        expression = "titleName",
        imports = ["xyz.laxus.util.titleName"]
    ),
    level = DeprecationLevel.WARNING
)
inline val <reified E: Enum<E>> E.niceName inline get() = titleName

/**
 * Returns a title-case formatted name of an [Enum].
 *
 * This is assuming that the Enum constant in question has a
 * name that follows typical java enum naming conventions.
 *
 * For example: `SOME_ENUM` becomes `Some Enum`.
 */
@get:JvmName("enumTitleNameFor")
val <E: Enum<E>> E.titleName: String get() {
    val chars = name.toCharArray()
    // Save a flag for when the next char should be capitalized
    var capitalize = true // First character is always gonna be capitalized
    for((i, c) in chars.withIndex()) {
        chars[i] = when {
            c == '_' -> {                  // c is an underscore
                capitalize = true           // set next character to be capitalized
                ' '                         // a space
            }
            capitalize && c.isLetter() -> { // c will be capitalized
                capitalize = false           // set next character to be lowercase
                c.toUpperCase()              // this character in upper case
            }
            else -> c.toLowerCase()         // this character in lower case
        }
    }

    // Efficiently, we create a character array from
    //the enum's name, and then a string from the
    //modified character array.
    // This isn't really much of an overhead save, however
    //the previous implementation had used regex splitting
    //which as it turns out wasn't that necessary.
    return String(chars)
}

/**
 * Hashes all the [objects] together and returns the result.
 *
 * This uses [Arrays.hashCode].
 *
 * @param objects Objects to merge into a combined hashcode.
 *
 * @return A hashcode from the resulting [objects].
 */
fun hashAll(vararg objects: Any?): Int = Arrays.hashCode(objects)