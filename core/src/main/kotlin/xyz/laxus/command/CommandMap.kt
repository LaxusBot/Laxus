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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package xyz.laxus.command

import xyz.laxus.util.collections.accumulate

class CommandMap(private vararg val commands: Command): Map<String, Command> {
    companion object {
        private const val ILLEGAL_INSERT = "Cannot register a Command with previously registered name: '%s'"
    }

    private val map: Map<String, Int>
    override val entries: Set<Map.Entry<String, Command>>

    init {
        val map = HashMap<String, Int>()
        for((i, c) in this.commands.sorted().withIndex()) {
            val name = c.name.toLowerCase()
            // Check to make sure it's not already registered
            require(name !in map) { ILLEGAL_INSERT.format(name) }
            // Map to index
            map[name] = i
            c.aliases.forEach {
                val alias = it.toLowerCase()
                // Check to make sure it's not already registered
                require(alias !in map) { ILLEGAL_INSERT.format(alias) }
                // Map to index
                map[alias] = i
            }
        }
        this.map = map
        this.entries = map.entries.mapTo(LinkedHashSet()) { Entry(it) }
    }

    constructor(vararg groups: Command.Group): this(*groups.accumulate { it.commands }.sorted().toTypedArray())

    override val size get() = commands.size
    override val keys get() = map.keys
    override val values get() = commands.toSet()

    operator fun contains(name: String): Boolean = containsKey(name)

    ///////////////
    // OVERRIDES //
    ///////////////

    override fun get(key: String): Command? = commands.getOrNull(map[key.toLowerCase()] ?: -1)
    override fun containsValue(value: Command): Boolean = commands.contains(value)
    override fun containsKey(key: String): Boolean = key.toLowerCase() in map
    override fun isEmpty(): Boolean = commands.isEmpty() && map.isEmpty()

    /////////////
    // PRIVATE //
    /////////////

    private inner class Entry(entry: Map.Entry<String, Int>): Map.Entry<String, Command> {
        override val key = entry.key
        override val value = commands[entry.value]
    }
}
