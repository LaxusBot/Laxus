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

@Deprecated(
    message = "Array.keyToMap((V) -> K) is deprecated.",
    replaceWith = ReplaceWith(
        expression = "Array.associateBy((T) -> K)"
    ),
    level = DeprecationLevel.WARNING
)
inline fun <reified K, reified V> Array<V>.keyToMap(function: (V) -> K): Map<K, V> {
    return associateBy(function)
    //return mapOf(*map { function(it) to it }.toTypedArray())
}

@Deprecated(
    message = "Iterable.keyToMap((V) -> K) is deprecated.",
    replaceWith = ReplaceWith(
        expression = "Iterable.associateBy((T) -> K)"
    ),
    level = DeprecationLevel.WARNING
)
inline fun <reified K, reified V> Iterable<V>.keyToMap(function: (V) -> K): Map<K, V> {
    return associateBy(function)
    //return mapOf(*map { function(it) to it }.toTypedArray())
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

@Deprecated(
    message = "Array.forAllButLast((T) -> Unit) is deprecated.",
    replaceWith = ReplaceWith(
        expression = "Array.forAllButLast((T) -> Unit, (T) -> Unit)",
        imports = ["xyz.laxus.util.collections.forAllButLast"]
    ),
    level = DeprecationLevel.WARNING
)
inline fun <reified T> Array<T>.forAllButLast(function: (T) -> Unit): T {
    require(isNotEmpty()) { "Cannot run on an empty array!" }
    forAllButLast(function) { return it }
    throw IllegalStateException("Failed to return element at last index of array!")
}

@Deprecated(
    message = "Collection.forAllButLast((T) -> Unit) is deprecated.",
    replaceWith = ReplaceWith(
        expression = "Collection.forAllButLast((T) -> Unit, (T) -> Unit)",
        imports = ["xyz.laxus.util.collections.forAllButLast"]
    ),
    level = DeprecationLevel.WARNING
)
inline fun <reified T> Collection<T>.forAllButLast(function: (T) -> Unit): T {
    require(isNotEmpty()) { "Cannot run on an empty array!" }
    forAllButLast(function) { return it }
    throw IllegalStateException("Failed to return element at last index of collection!")
}

inline fun <reified T> Array<T>.forAllButLast(function: (T) -> Unit, last: (T) -> Unit) {
    if(isEmpty()) return
    val lastIndex = lastIndex
    for((i, e) in this.withIndex()) {
        if(i < lastIndex) {
            function(e)
        } else {
            last(e)
        }
    }
}

inline fun <reified T> Collection<T>.forAllButLast(function: (T) -> Unit, last: (T) -> Unit) {
    if(isEmpty()) return
    val lastIndex = size - 1
    for((i, e) in this.withIndex()) {
        if(i < lastIndex) {
            function(e)
        } else {
            last(e)
        }
    }
}

fun <T> Array<T>.swap(i1: Int, i2: Int) {
    val temp = this[i1]
    this[i1] = this[i2]
    this[i2] = temp
}

fun <T> MutableList<T>.swap(i1: Int, i2: Int) {
    val v1 = this[i1]
    val v2 = this[i2]
    this[i1] = v2
    this[i2] = v1
}

infix fun <T> Array<T>.swap(indices: Pair<Int, Int>) {
    val (first, second) = indices
    swap(first, second)
}
infix fun <T> MutableList<T>.swap(indices: Pair<Int, Int>) {
    val (first, second) = indices
    swap(first, second)
}

// Shortcuts

fun <T> unmodifiableList(list: List<T>): List<T> {
    return Collections.unmodifiableList(list)
}

fun <T> unmodifiableList(vararg elements: T): List<T> {
    return FixedSizeArrayList(*elements)
}

fun <T> unmodifiableSet(set: Set<T>): Set<T> {
    return Collections.unmodifiableSet(set)
}

// Note that T: Any is because ConcurrentHashMap.newKeySet() only
// supports non-null entries.
fun <T: Any> concurrentSet(): MutableSet<T> {
    return ConcurrentHashMap.newKeySet()
}

fun <T: Any> concurrentSet(vararg elements: T): MutableSet<T> {
    return concurrentSet<T>().also { it += elements }
}

// Note that T: Any is because ConcurrentHashMap only
// supports non-null entries.
fun <K: Any, V: Any> concurrentHashMap(): ConcurrentHashMap<K, V> {
    return ConcurrentHashMap()
}

fun <K: Any, V: Any> concurrentHashMap(vararg pairs: Pair<K, V>): ConcurrentHashMap<K, V> {
    return concurrentHashMap<K, V>().also { it += pairs }
}

// O(2n)
inline fun <reified T> Array<T?>.filterNulls(): Array<T> {
    val notNull = count { it !== null }     // Count how many are not null in the array
    if(notNull == 0) return emptyArray()    // All elements are null
    var index = 0                           // Start with an index 0
    return Array<T>(notNull) new@ { i ->
        while(index <= lastIndex) {         // While we haven't passed the last index of the original Array
            val e = this[index]             // Retrieve element at index
            index++                         // Increment
            if(e !== null) {                // If it's not null
                return@new e                // return it as the element at "i" in this new Array
            }
        }
        // If somehow this fails, throw an AssertionError
        throw AssertionError("Could not find element for index '${index - 1}' to be inserted at '$i'!")
    }
}
