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

inline fun <reified T> T.modifyIf(condition: Boolean, block: (T) -> T): T = if(condition) block(this) else this
inline fun <reified T> T.modifyUnless(condition: Boolean, block: (T) -> T): T = modifyIf(!condition, block)

inline val <reified E: Enum<E>> E.niceName: String inline get() {
    val parts = name.split('_')
    return parts.joinToString(" ") join@ {
        if(it.length < 2) {
            return@join it.toUpperCase()
        }

        "${it[0].toUpperCase()}${it.substring(1).toLowerCase()}"
    }
}

fun hashAll(vararg objects: Any?): Int = Arrays.hashCode(objects)