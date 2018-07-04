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

import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.Experiment
import xyz.laxus.util.db.afkMessage

/**
 * @author Kaidan Gustave
 */
@Experiment("AFK is an experimental feature!")
class AFKCommand: Command(StandardGroup) {
    override val name = "AFK"
    override val arguments = "<AFK Message>"
    override val help = "Sets the bot to auto-respond to mentions while you are away."

    override suspend fun execute(ctx: CommandContext) {
        val args = ctx.args
        if(args.length > 500) return ctx.replyError {
            "Cannot set an AFK message greater than 500 characters!"
        }
        ctx.author.afkMessage = args
        ctx.reply("${ctx.author.asMention} has gone AFK")
    }
}