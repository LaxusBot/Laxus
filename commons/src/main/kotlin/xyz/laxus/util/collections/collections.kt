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
package xyz.laxus.util.collections

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

// Collections
inline fun <reified T, reified R> Array<out T>.accumulate(function: (T) -> Collection<R>): List<R> {
    return when {
        this.isEmpty() -> emptyList()
        this.size == 1 -> function(this[0]).toList()
        else -> {
            val list = ArrayList<R>()
            for(element in this) {
                list += function(element)
            }
            return list
        }
    }
}

inline fun <reified T, reified R> Collection<T>.accumulate(function: (T) -> Collection<R>): List<R> {
    return when {
        this.isEmpty() -> emptyList()
        this.size == 1 -> function(this.first()).toList()
        else -> {
            val list = ArrayList<R>()
            for(element in this) {
                list += function(element)
            }
            return list
        }
    }
}

inline fun <reified K, reified V> Array<V>.keyToMap(function: (V) -> K): Map<K, V> {
    return mapOf(*map { function(it) to it }.toTypedArray())
}

inline fun <reified K, reified V> Iterable<V>.keyToMap(function: (V) -> K): Map<K, V> {
    return mapOf(*map { function(it) to it }.toTypedArray())
}

inline fun <reified K, reified V> Array<V>.multikeyToMap(function: (V) -> Iterable<K>): Map<K, V> {
    val map = HashMap<K, V>()
    forEach { v ->
        function(v).forEach { k ->
            map[k] = v
        }
    }
    return map
}

inline fun <reified K, reified V> Iterable<V>.multikeyToMap(function: (V) -> Iterable<K>): Map<K, V> {
    val map = HashMap<K, V>()
    forEach { v ->
        function(v).forEach { k ->
            map[k] = v
        }
    }
    return map
}

inline fun <reified T> Array<T>.sumByLong(transform: (T) -> Long): Long = map(transform).sum()

inline fun <reified T> Iterable<T>.sumByLong(transform: (T) -> Long): Long = map(transform).sum()

inline fun <reified T> Array<T>.forAllButLast(function: (T) -> Unit): T {
    require(isNotEmpty()) { "Cannot run on an empty array!" }
    val lastIndex = lastIndex
    for((i, e) in this.withIndex()) {
        if(i < lastIndex)
            function(e)
        else
            return e
    }
    throw IllegalStateException("Failed to return element at last index of array!")
}

inline fun <reified T> Collection<T>.forAllButLast(function: (T) -> Unit): T {
    require(isNotEmpty()) { "Cannot run on an empty array!" }
    val lastIndex = size - 1
    for((i, e) in this.withIndex()) {
        if(i < lastIndex)
            function(e)
        else
            return e
    }
    throw IllegalStateException("Failed to return element at last index of collection!")
}

// Shortcuts

fun <T> unmodifiableList(list: List<T>): List<T> {
    return Collections.unmodifiableList(list)
}

fun <T> unmodifiableList(vararg elements: T): List<T> {
    return FixedSizeArrayList(*elements)
}

fun <T: Any> concurrentSet(): MutableSet<T> {
    return ConcurrentHashMap.newKeySet()
}

fun <T: Any> concurrentSet(vararg elements: T): MutableSet<T> {
    return concurrentSet<T>().also { it += elements }
}

fun <K: Any, V: Any> concurrentHashMap(): ConcurrentHashMap<K, V> {
    return ConcurrentHashMap()
}

fun <K: Any, V: Any> concurrentHashMap(vararg pairs: Pair<K, V>): ConcurrentHashMap<K, V> {
    return concurrentHashMap<K, V>().also { it += pairs }
}
