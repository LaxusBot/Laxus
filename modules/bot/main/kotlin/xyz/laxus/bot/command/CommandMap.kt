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
package xyz.laxus.bot.command

@Deprecated("Replaced with a normal map")
class CommandMap(vararg commands: Command): Map<String, Command> {
    private val map: Map<String, Command>

    init {
        this.map = hashMapOf()
        for(command in commands.sorted()) {
            val name = command.name.toLowerCase()
            // Check to make sure it's not already registered
            require(name !in map) { ILLEGAL_INSERT.format(name) }
            // Map to index
            map[name] = command
            for(alias in command.aliases.asSequence().map { it.toLowerCase() }) {
                // Check to make sure it's not already registered
                require(alias !in map) { ILLEGAL_INSERT.format(alias) }
                // Map to index
                map[alias] = command
            }
        }
    }

    override val size get() = map.size
    override val keys get() = map.keys
    override val values get() = map.values
    override val entries get() = map.entries

    operator fun contains(name: String): Boolean = containsKey(name.toLowerCase())

    ///////////////
    // OVERRIDES //
    ///////////////

    override fun get(key: String): Command? = map[key.toLowerCase()]
    override fun containsValue(value: Command): Boolean = map.containsValue(value)
    override fun containsKey(key: String): Boolean = key.toLowerCase() in map
    override fun isEmpty(): Boolean = map.isEmpty()

    /////////////
    // PRIVATE //
    /////////////

    private companion object {
        private const val ILLEGAL_INSERT =
            "Cannot register a Command with previously registered name: '%s'"
    }
}
