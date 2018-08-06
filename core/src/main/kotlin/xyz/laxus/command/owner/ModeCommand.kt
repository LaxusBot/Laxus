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

import xyz.laxus.RunMode
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext

/**
 * @author Kaidan Gustave
 */
class ModeCommand: Command(OwnerGroup) {
    override val name = "Mode"
    override val aliases = arrayOf("RunMode")
    override val arguments = "<RunMode>"
    override val help = "Sets the bot's run mode."
    override val guildOnly = false
    override val hasAdjustableLevel = false

    override suspend fun execute(ctx: CommandContext) {
        val args = ctx.args
        if(args.isEmpty()) return ctx.reply {
            "I am currently set to **${ctx.bot.mode}**!"
        }
        val mode = RunMode.valueOf(args.toUpperCase())
        if(ctx.bot.mode == mode) return ctx.replyWarning {
            "I am already set to **$mode**!"
        }
        ctx.bot.setMode(mode)
        ctx.replySuccess("Successfully set mode to **$mode**!")
    }
}