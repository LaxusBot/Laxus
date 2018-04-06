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

import java.util.*

/**
 * Returns `null` typed as [T?][T].
 *
 * This is effective when working with certain operator
 * functions that use type arguments and do not have
 * explicit type specification abilities.
 *
 * @return `null` typed as [T?][T].
 */
fun <T> nullOf(): T? = null

/**
 * Modifies the receiver based on the provided [block]
 * if [condition] returns `true`, or else returns the
 * receiver unchanged.
 */
inline fun <reified T> T.modifyIf(condition: (T) -> Boolean, block: (T) -> T): T = modifyIf(condition(this), block)

/**
 * Modifies the receiver based on the provided [block]
 * unless [condition] returns `true`, in which case this
 * returns the receiver unchanged.
 */
inline fun <reified T> T.modifyUnless(condition: (T) -> Boolean, block: (T) -> T): T = modifyUnless(condition(this), block)

/**
 * Modifies the receiver based on the provided [block]
 * if [condition] is `true`, or else returns the
 * receiver unchanged.
 */
inline fun <reified T> T.modifyIf(condition: Boolean, block: (T) -> T): T = if(condition) block(this) else this

/**
 * Modifies the receiver based on the provided [block]
 * unless [condition] is `true`, in which case this
 * returns the receiver unchanged.
 */
inline fun <reified T> T.modifyUnless(condition: Boolean, block: (T) -> T): T = modifyIf(!condition, block)

/**
 * Returns a nicely formatted name of an enum.
 *
 * This is assuming that the Enum constant in question
 * has a name that follows typical java enum naming
 * conventions. For example: `SOME_ENUM` becomes
 * `Some Enum`.
 */
inline val <reified E: Enum<E>> E.niceName inline get() = name.split('_').joinToString(" ") join@ {
    if(it.length < 2) return@join it.toUpperCase()
    return@join "${it[0].toUpperCase()}${it.substring(1).toLowerCase()}"
}

/**
 * Hashes all the [objects] together and returns the result.
 *
 * This uses [Arrays.hashCode].
 */
fun hashAll(vararg objects: Any?): Int = Arrays.hashCode(objects)