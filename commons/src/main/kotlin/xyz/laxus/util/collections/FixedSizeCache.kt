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
package xyz.laxus.util.collections

import java.util.function.BiConsumer

@Suppress("UNCHECKED_CAST")
class FixedSizeCache<K, V>(size: Int): MutableMap<K, V> {
    init {
        require(size>0) { "Cache size must be at least 1!" }
    }

    private var currIndex = 0

    private val map: MutableMap<K, V> = HashMap()
    private val backingKeys: Array<K?> = arrayOfNulls<Any>(size) as Array<K?>

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = map.entries

    override val keys: MutableSet<K>
        get() = map.keys

    override val values: MutableCollection<V>
        get() = map.values

    override val size: Int
        get() = backingKeys.count { it !== null }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    override fun clear() {
        map.clear()
        for(i in 0 until backingKeys.size)
            backingKeys[i] = null
        currIndex = 0
    }

    override fun put(key: K, value: V): V? {
        val v = if(backingKeys[currIndex] !== null)
            map.remove(backingKeys[currIndex])
        else map.put(key, value)

        backingKeys[currIndex] = key
        currIndex = (currIndex + 1) % backingKeys.size
        return v
    }

    override fun putAll(from: Map<out K, V>) = from.forEach { k, v -> put(k,v) }

    override fun remove(key: K): V? = map.remove(key)

    override fun isEmpty(): Boolean = size == 0

    override fun get(key: K): V? = map[key]

    override fun getOrDefault(key: K, defaultValue: V): V = map[key] ?: defaultValue

    override fun containsKey(key: K): Boolean = map.containsKey(key)

    override fun containsValue(value: V): Boolean = map.containsValue(value)

    override fun forEach(action: BiConsumer<in K, in V>) = map.forEach(action)
}
