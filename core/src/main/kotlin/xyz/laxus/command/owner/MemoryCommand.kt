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

import net.dv8tion.jda.core.Permission
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.util.embed
import xyz.laxus.util.runtime
import xyz.laxus.util.totalMemory
import xyz.laxus.util.freeMemory
import xyz.laxus.util.maxMemory

/**
 * @author Kaidan Gustave
 */
class MemoryCommand: Command(OwnerGroup) {
    private companion object {
        private const val mb = 1024 * 1024
    }

    override val name = "Memory"
    override val help = "Gets Laxus's runtime memory statistics."
    override val hasAdjustableLevel = false
    override val botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)

    override suspend fun execute(ctx: CommandContext) {
        val runtime = runtime

        val embed = embed {
            title { "Laxus Runtime Statistics" }
            if(ctx.isGuild) color { ctx.selfMember.color }
            appendln("```ini")
            appendln("[ Current Memory Usage ]     ${(runtime.totalMemory - runtime.freeMemory) / mb}mb")
            appendln("[ Free Memory Available ]    ${runtime.freeMemory / mb}mb")
            appendln("[ Total Memory Usage ]       ${runtime.totalMemory / mb}mb")
            append("[ Maximum Memory Available ] ${runtime.maxMemory / mb}mb")
            append("```")
        }

        ctx.reply(embed)
    }

}
