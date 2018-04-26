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
@file:Suppress("UNCHECKED_CAST")
package xyz.laxus.util.functional

operator fun String.Companion.get(vararg strings: String) = arrayOf(*strings)
operator fun Int.Companion.get(vararg ints: Int) = intArrayOf(*ints)
operator fun Long.Companion.get(vararg longs: Long) = longArrayOf(*longs)
operator fun Byte.Companion.get(vararg bytes: Byte) = byteArrayOf(*bytes)

interface ArrayOperator<T: Any> {
    operator fun get(vararg elements: T): Array<T> {
        val array = arrayOfNulls<Any>(elements.size)
        for(i in 0 until array.size) {
            array[i] = elements[i]
        }
        return array as Array<T>
    }
}