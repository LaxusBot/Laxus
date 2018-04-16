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
package xyz.laxus.command.standard

import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.util.await
import xyz.laxus.util.formattedName

/**
 * @author Kaidan Gustave
 */
class HelpCommand : Command(StandardGroup) {
    override val name = "Help"
    override val help = "Gets a list of all commands."
    override val guildOnly = false
    override val hasAdjustableLevel = false

    override suspend fun execute(ctx: CommandContext) {
        val prefix = ctx.bot.prefix
        val message = buildString {
            appendln("**Available Commands in ${if(ctx.isGuild) ctx.textChannel.asMention else "Direct Messages"}**")
            ctx.bot.groups.forEach g@ { g ->
                // They can't use the command group so we don't display it here
                if(!g.check(ctx))
                    return@g

                val available = g.commands.filter { with(it) { ctx.level.test(ctx) } }

                if(available.isEmpty())
                    return@g

                appendln()
                appendln("__${g.name} Commands__")
                appendln()

                available.forEach c@ { c ->
                    append("`").append(prefix).append(c.name)
                    val arguments = c.arguments
                    if(arguments.isNotBlank()) {
                        append(" $arguments")
                    }
                    append("` - ${c.help}")
                    if(c.isExperimental) append(" `[EXPERIMENTAL]`")
                    appendln()
                }
            }

            val shen = ctx.jda.retrieveUserById(Laxus.DevId).await()
            appendln()
            append("For additional help contact ${shen.formattedName(true)} or join " +
                   "my support server: **<${Laxus.ServerInvite}>**")
        }

        if(ctx.isGuild)
            ctx.reactSuccess()
        ctx.replyInDM(message)
    }
}
