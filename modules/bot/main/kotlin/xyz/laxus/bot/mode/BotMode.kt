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
package xyz.laxus.bot.mode

import ch.qos.logback.classic.Level
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import xyz.laxus.bot.Bot
import xyz.laxus.bot.command.Command
import xyz.laxus.bot.command.CommandContext
import xyz.laxus.bot.command.CommandError

interface BotMode {
    val level: Level

    fun onAttach(bot: Bot) {}

    fun onCommandCall(ctx: CommandContext, command: Command) {}

    fun onCommandComplete(ctx: CommandContext, command: Command) {}

    fun onCommandTerminated(ctx: CommandContext, command: Command, text: String) {}

    fun onCommandError(ctx: CommandContext, command: Command, error: CommandError) {}

    fun onDetach(bot: Bot) {}

    fun interceptCall(event: MessageReceivedEvent, bot: Bot, name: String, args: String): Boolean = true
}
