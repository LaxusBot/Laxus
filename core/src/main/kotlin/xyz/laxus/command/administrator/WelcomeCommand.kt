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
package xyz.laxus.command.administrator

import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.EmptyCommand
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.findTextChannels
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.hasWelcome
import xyz.laxus.util.db.welcome
import xyz.laxus.util.multipleTextChannels
import xyz.laxus.util.noMatch

/**
 * @author Kaidan Gustave
 */
class WelcomeCommand: EmptyCommand(AdministratorGroup) {
    private companion object {
        private const val MESSAGE_MAX_LENGTH = 1900
    }

    override val name = "Welcome"
    override val help = "Manages the server's welcome system."
    override val children = arrayOf(
        WelcomeRawCommand(),
        WelcomeRemoveCommand(),
        WelcomeSetCommand()
    )

    private inner class WelcomeRawCommand : Command(this@WelcomeCommand) {
        override val name = "Raw"
        override val help = "Gets the raw value for the server's welcome message."
        override val defaultLevel = Level.STANDARD

        override suspend fun execute(ctx: CommandContext) {
            val message = ctx.guild.welcome?.second ?: return ctx.replyError {
                "This server has not setup the welcome system!"
            }

            ctx.reply("**Welcome message for ${ctx.guild.name}:** ```\n$message```")
        }
    }

    private inner class WelcomeRemoveCommand : Command(this@WelcomeCommand) {
        override val name = "Remove"
        override val help = "Gets a list of all the available custom commands."

        override suspend fun execute(ctx: CommandContext) {
            if(!ctx.guild.hasWelcome) return ctx.replyError {
                "This server has not setup the welcome system!"
            }

            ctx.guild.welcome = null
            ctx.replySuccess("Successfully removed welcome for this server!")
        }
    }

    @MustHaveArguments("Specify a channel and a message to send when users join your server!")
    private inner class WelcomeSetCommand : Command(this@WelcomeCommand) {
        override val name = "Set"
        override val arguments = "[Channel] [Welcome Message]"
        override val help = "Sets the server's welcome system."

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)

            if(parts.size < 2) return ctx.missingArgs {
                "Please specify a channel and a welcome message!"
            }

            val found = ctx.guild.findTextChannels(parts[0])
            val channel = when {
                found.isEmpty() -> return ctx.replyError(noMatch("text channels", parts[0]))
                found.size > 1 -> return ctx.replyError(found.multipleTextChannels(parts[0]))
                else -> found[0]
            }

            if(!channel.canTalk()) return ctx.replyError {
                "I am not able to speak in ${channel.asMention}!"
            }

            val message = parts[1]

            if(message.length > MESSAGE_MAX_LENGTH) return ctx.replyError {
                "Welcome message content cannot exceed 1900 characters in length!"
            }

            ctx.guild.welcome = channel to message
            ctx.replySuccess("Successfully set welcome message for this server!")
        }
    }
}