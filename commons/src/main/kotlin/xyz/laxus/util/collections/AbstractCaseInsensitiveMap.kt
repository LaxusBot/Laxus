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

/**
 * @author Kaidan Gustave
 */
abstract class AbstractCaseInsensitiveMap<V> protected constructor(
    private val map: MutableMap<String, V>,
    private val savedEntries: MutableSet<MutableMap.MutableEntry<String, V>>
): MutableMap<String, V> {
    override val size get() = map.size
    override val entries get() = savedEntries
    override val keys get() = savedEntries.mapTo(hashSetOf()) { it.key }
    override val values get() = savedEntries.mapTo(hashSetOf()) { it.value }

    override fun containsKey(key: String): Boolean = map.containsKey(key.toLowerCase())
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun get(key: String): V? = map[key.toLowerCase()]
    override fun isEmpty(): Boolean = map.isEmpty() && entries.isEmpty()

    override fun clear() {
        savedEntries.clear()
        map.clear()
    }

    override fun put(key: String, value: V): V? {
        savedEntries.add(Entry(key, value))
        return map.put(key.toLowerCase(), value)
    }

    override fun putAll(from: Map<out String, V>) {
        from.forEach { this[it.key] = it.value }
    }

    override fun remove(key: String): V? = map.remove(key.toLowerCase())

    private inner class Entry(override val key: String, value: V): MutableMap.MutableEntry<String, V> {
        override var value = value
            get() = map[key.toLowerCase()]?.also {
                if(field != it) field = it
            } ?: field
            set(value) {
                map[key.toLowerCase()] = value
                field = value
            }

        override fun setValue(newValue: V): V {
            val oldValue = value
            value = newValue
            return oldValue
        }
    }
}