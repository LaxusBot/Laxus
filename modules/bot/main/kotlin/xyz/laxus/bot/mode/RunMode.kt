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
import xyz.laxus.bot.Bot
import xyz.laxus.bot.Laxus
import xyz.laxus.bot.command.Command
import xyz.laxus.bot.command.CommandContext
import xyz.laxus.bot.command.CommandError
import net.dv8tion.jda.core.events.message.MessageReceivedEvent as MRE

enum class RunMode(override val level: Level): BotMode {
    SERVICE(Level.INFO),

    IDLE(Level.ERROR) {
        override fun interceptCall(event: MRE, bot: Bot, name: String, args: String): Boolean {
            return Laxus.DevId == event.author.idLong
        }
    },

    DEBUG(Level.DEBUG) {
        override fun onAttach(bot: Bot) {
            Bot.Log.debug("Attaching Debugger")
        }

        override fun onCommandCall(ctx: CommandContext, command: Command) {
            Bot.Log.debug("Call made for command '${command.fullname}'")
        }

        override fun onCommandComplete(ctx: CommandContext, command: Command) {
            Bot.Log.debug("Call to command '${command.fullname}' completed!")
        }

        override fun onCommandTerminated(ctx: CommandContext, command: Command, text: String) {
            Bot.Log.debug("Terminated Command '${command.fullname}' with message: \"$text\"")
        }

        override fun onCommandError(ctx: CommandContext, command: Command, error: CommandError) {
            Bot.Log.debug("Call to command '${command.fullname}' resulted in an error: ", error)
        }

        override fun onDetach(bot: Bot) {
            Bot.Log.debug("Detaching Debugger")
        }
    }
}
