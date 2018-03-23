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

val Int.name: String? get() = Character.getName(this)
val Long.length: Int get() = "$this".length

fun Int.toChars(): CharArray = Character.toChars(this)
fun Int.toHexString(): String = Integer.toHexString(this)

fun emptyByteArray(): ByteArray = ByteArray(0)
fun emptyShortArray(): ShortArray = ShortArray(0)
fun emptyIntArray(): IntArray = IntArray(0)
fun emptyLongArray(): LongArray = LongArray(0)

fun arrayOf(vararg bytes: Byte): ByteArray = bytes
fun arrayOf(vararg shorts: Short): ShortArray = shorts
fun arrayOf(vararg ints: Int): IntArray = ints
fun arrayOf(vararg longs: Long): LongArray = longs
