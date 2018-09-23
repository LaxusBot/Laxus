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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.laxus.bot.command

import com.typesafe.config.Config
import net.dv8tion.jda.core.JDABuilder
import xyz.laxus.bot.Laxus

abstract class CommandGroup
protected constructor(val name: String): Comparable<CommandGroup> {
    abstract val defaultLevel: CommandLevel
    abstract val guildOnly: Boolean
    abstract val devOnly: Boolean
    open val unlisted = false
    val commands = arrayListOf<Command>()

    // Can be used as an arbitrary check for commands under group
    open fun check(ctx: CommandContext): Boolean = true
    open fun JDABuilder.configure() {}
    open fun dispose() {}

    operator fun Command.unaryPlus() {
        Laxus.Log.debug("Adding Command '${this.name}' to Group '${this@CommandGroup.name}'")
        commands += this
    }

    abstract fun init(config: Config)

    override fun compareTo(other: CommandGroup): Int {
        return defaultLevel.compareTo(other.defaultLevel).takeIf { it != 0 } ?:
               name.compareTo(other.name)
    }
}
