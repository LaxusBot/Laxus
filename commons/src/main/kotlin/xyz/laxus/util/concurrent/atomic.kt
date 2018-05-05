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
@file:JvmName("AtomicKt")
@file:Suppress("unused", "NOTHING_TO_INLINE", "FunctionName")
package xyz.laxus.util.concurrent

import java.util.concurrent.atomic.AtomicBoolean   as JVMAtomicBoolean
import java.util.concurrent.atomic.AtomicInteger   as JVMAtomicInt
import java.util.concurrent.atomic.AtomicLong      as JVMAtomicLong
import java.util.concurrent.atomic.AtomicReference as JVMAtomicRef

typealias AtomicRef<V>  = JVMAtomicRef<V>
typealias AtomicInt     = JVMAtomicInt
typealias AtomicLong    = JVMAtomicLong
typealias AtomicBoolean = JVMAtomicBoolean

fun <V> atomicRef(initial: V) = AtomicRef<V>(initial)
fun atomicInt(initial: Int = 0) = AtomicInt(initial)
fun atomicLong(initial: Long = 0) = AtomicLong(initial)
fun atomicBoolean(initial: Boolean = false) = AtomicBoolean(initial)

inline operator fun JVMAtomicInt.inc() = apply { getAndIncrement() }
inline operator fun JVMAtomicInt.plusAssign(value: Int) { getAndAdd(value) }
inline operator fun JVMAtomicInt.minusAssign(value: Int) { getAndAdd(-value) }
inline operator fun JVMAtomicInt.plus(value: Int) = get() + value
inline operator fun JVMAtomicInt.minus(value: Int) = get() - value

inline operator fun JVMAtomicLong.inc() = apply { getAndIncrement() }
inline operator fun JVMAtomicLong.plusAssign(value: Long) { getAndAdd(value) }
inline operator fun JVMAtomicLong.minusAssign(value: Long) { getAndAdd(-value) }
inline operator fun JVMAtomicLong.plus(value: Long) = get() + value
inline operator fun JVMAtomicLong.minus(value: Long) = get() - value
