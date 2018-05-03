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
package xyz.laxus.command.owner

import net.dv8tion.jda.core.Permission.*
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.menus.updatingMenu
import xyz.laxus.jda.menus.updatingMenuBuilder
import xyz.laxus.jda.util.embed
import xyz.laxus.util.*
import xyz.laxus.util.concurrent.duration
import java.util.concurrent.TimeUnit.*

/**
 * @author Kaidan Gustave
 */
class MemoryCommand: Command(OwnerGroup) {
    private companion object {
        private const val mb = 1048576L // 1 mb value in kb
        private const val up = "\uD83D\uDD3C"
        private const val down = "\uD83D\uDD3D"
        private const val same = "\u23F8"
    }

    override val name = "Memory"
    override val help = "Gets Laxus's runtime memory statistics."
    override val hasAdjustableLevel = false
    override val botPermissions = arrayOf(MESSAGE_EMBED_LINKS)
    override val children = arrayOf<Command>(
        MemorySnapshotCommand()
    )

    private val builder = updatingMenuBuilder {
        interval = duration(3, SECONDS)
        waiter = Laxus.Waiter
        text = "${Laxus.Success} Memory:"
        timeout {
            delay { 2 }
            unit { MINUTES }
        }
    }

    override suspend fun execute(ctx: CommandContext) {
        val runtime = runtime
        var lastMemory: Memory? = null
        val menu = updatingMenu(builder) {
            update {
                val memory = runtime.memory
                val last = lastMemory ?: memory
                title { "Laxus Runtime Statistics" }
                code("ini") {
                    val current = (memory.total - memory.free) / mb
                    val free = memory.free / mb
                    val total = memory.total / mb
                    val max = memory.max / mb
                    + "[ Current Usage ] ${current}mb ${arrow(current, (last.total - last.free) / mb)}\n"
                    + "[ Free ]          ${free}mb ${arrow(free, last.free / mb)}\n"
                    + "[ Total Usage ]   ${total}mb ${arrow(total, last.total / mb)}\n"
                    + "[ Maximum ]       ${max}mb ${arrow(max, last.max / mb)}"
                }
                color { ctx.selfMember.color }
                lastMemory = memory
            }
            finalAction { message ->
                if(message.guild.selfMember.hasPermission(message.textChannel, MESSAGE_MANAGE)) {
                    message.clearReactions().queue()
                } else {
                    // Clear our reactions
                    message.reactions.forEach { it.removeReaction().queue() }
                }
            }
        }

        menu.displayIn(ctx.channel)

        /*
        val memory = runtime.memory
        val embed = embed {
            title { "Laxus Runtime Statistics" }
            code("ini") {
                + "[ Current Memory Usage ]     ${(memory.total - memory.free) / mb}mb\n"
                + "[ Free Memory Available ]    ${memory.free / mb}mb\n"
                + "[ Total Memory Usage ]       ${memory.total / mb}mb\n"
                + "[ Maximum Memory Available ] ${memory.max / mb}mb"
            }
            if(ctx.isGuild) color { ctx.selfMember.color }
        }
        ctx.reply(embed)
        */
    }

    private fun arrow(now: Long, last: Long): String {
        return when {
            now < last -> down
            now > last -> up
            else       -> same
        }
    }

    private inner class MemorySnapshotCommand: Command(this@MemoryCommand) {
        override val name = "Snapshot"
        override val help = "Gets a snapshot of Laxus's memory."
        override val hasAdjustableLevel = false
        override val botPermissions = arrayOf(MESSAGE_EMBED_LINKS)

        override suspend fun execute(ctx: CommandContext) {
            val memory = runtime.memory
            val embed = embed {
                title { "Laxus Runtime Statistics" }
                code("ini") {
                    + "[ Current Memory Usage ]     ${(memory.total - memory.free) / mb}mb\n"
                    + "[ Free Memory Available ]    ${memory.free / mb}mb\n"
                    + "[ Total Memory Usage ]       ${memory.total / mb}mb\n"
                    + "[ Maximum Memory Available ] ${memory.max / mb}mb"
                }
                if(ctx.isGuild) color { ctx.selfMember.color }
            }
            ctx.reply(embed)
        }
    }
}
