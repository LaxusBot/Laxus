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
package xyz.laxus.bot.command.standard

import xyz.laxus.bot.command.Command
import xyz.laxus.bot.command.CommandContext
import xyz.laxus.bot.command.commandErrorIf
import xyz.laxus.bot.utils.Emojis
import xyz.laxus.bot.utils.db.afkMessage
import xyz.laxus.bot.utils.jda.boldFormattedName

class AFKCommand: Command(StandardGroup) {
    override val name = "AFK"
    override val arguments = "<AFK Message>"
    override val help = "Sets the bot to auto-respond to mentions while you are away."

    override suspend fun execute(ctx: CommandContext) {
        val args = ctx.args
        commandErrorIf(args.length > 500) { "Cannot set an AFK message greater than 500 characters!" }
        ctx.author.afkMessage = args
        ctx.reply("${Emojis.ZZZ} ${ctx.author.boldFormattedName} has gone AFK")
    }
}
