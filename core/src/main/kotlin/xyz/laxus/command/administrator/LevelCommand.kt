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

import xyz.laxus.command.*
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.getCommandLevel
import xyz.laxus.util.db.setCommandLevel
import xyz.laxus.util.titleName

/**
 * @author Kaidan Gustave
 */
class LevelCommand: EmptyCommand(AdministratorGroup) {
    override val name = "Level"
    override val help = "Manage command usage permissions."
    override val children = arrayOf(
        LevelResetCommand(),
        LevelSetCommand()
    )

    private fun levelFromArgs(args: String): Level? {
        return when(args.toLowerCase()) {
            "standard", "public" -> Level.STANDARD
            "mod", "moderator" -> Level.MODERATOR
            "admin", "administrator" -> Level.ADMINISTRATOR
            "owner" -> Level.SERVER_OWNER
            else -> null
        }
    }

    @MustHaveArguments("Specify a command whose level you want to reset.")
    private inner class LevelResetCommand: Command(this@LevelCommand) {
        override val name = "Reset"
        override val arguments = "[Command]"
        override val help = "Resets the level of a command for the server."

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            val command = ctx.bot.searchCommand(args) ?: return ctx.replyError {
                "Could not find a command named `$args`!"
            }

            val currentLevel = ctx.guild.getCommandLevel(command) ?: command.defaultLevel

            if(currentLevel == command.defaultLevel) return ctx.replyError {
                "`${command.fullname}` is already set to ${command.defaultLevel.titleName}"
            }

            ctx.guild.setCommandLevel(command, null)
            ctx.replySuccess("`${command.fullname}` level reset.")
        }
    }

    @MustHaveArguments("Specify a command to set the level for, and a level to use.")
    private inner class LevelSetCommand: Command(this@LevelCommand) {
        override val name = "Set"
        override val arguments = "[Command] [Level]"
        override val help = "Sets the level of a command for the server."

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            val splitArgs = args.split(commandArgs)

            val commandArgs = splitArgs.subList(0, splitArgs.lastIndex).joinToString(" ")
            val levelArgs = splitArgs.last()

            val command = ctx.bot.searchCommand(commandArgs) ?: return ctx.replyError {
                "Could not find a command named `$commandArgs`!"
            }

            if(!command.hasAdjustableLevel) return ctx.replyError {
                "`${command.fullname}` does not allow for adjustment of command level!"
            }

            val level = levelFromArgs(levelArgs) ?: return ctx.replyError {
                "`$levelArgs` is not a valid command level!"
            }

            // Are we setting back to the default level manually
            // instead of using the Level Reset command
            val resetting = command.defaultLevel == level

            if(!resetting) {
                val currentLevel = ctx.guild.getCommandLevel(command)

                if(level == currentLevel) return ctx.replyError {
                    "`${command.fullname}` is already set to ${level.titleName}"
                }

                if(command.defaultLevel.ordinal > level.ordinal) return ctx.replyError {
                    "Cannot set level of `${command.fullname}` lower than ${command.defaultLevel.titleName}!"
                }
            }

            ctx.guild.setCommandLevel(command, if(resetting) null else level)
            ctx.replySuccess("`${command.fullname}` level ${if(resetting) "reset" else "set to ${level.titleName}"}.")
        }
    }
}
