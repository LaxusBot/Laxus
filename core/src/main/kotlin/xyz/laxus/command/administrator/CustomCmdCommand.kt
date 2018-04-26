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

import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.EmptyCommand
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.db.entities.Tag
import xyz.laxus.jda.menus.paginator
import xyz.laxus.jda.menus.paginatorBuilder
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.*

/**
 * @author Kaidan Gustave
 */
class CustomCmdCommand: EmptyCommand(AdministratorGroup) {
    companion object {
        private const val NAME_MAX_LENGTH = 50
        private const val CONTENT_MAX_LENGTH = 1900
    }

    override val name = "CustomCmd"
    override val help = "Manage the server's custom commands."
    override val aliases = arrayOf("CustomCommand", "CC")
    override val children = arrayOf(
        CustomCmdAddCommand(),
        CustomCmdEditCommand(),
        CustomCmdImportCommand(),
        CustomCmdListCommand(),
        CustomCmdRemoveCommand()
    )

    @MustHaveArguments("Specify arguments in the format `%arguments`.")
    private inner class CustomCmdAddCommand : Command(this@CustomCmdCommand) {
        override val name = "Add"
        override val arguments = "[Command Name] [Command Content]"
        override val help = "Adds a custom command."
        override val cooldown = 30
        override val cooldownScope = CooldownScope.GUILD

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)

            when {
                parts[0].length > Tag.MaxNameLength -> return ctx.replyError {
                    "Custom Command names cannot exceed ${Tag.MaxNameLength} characters in length!"
                }
                ctx.bot.searchCommand(parts[0]) !== null -> return ctx.replyError {
                    "Custom Commands may not have names that match standard command names!"
                }
                parts.size == 1 -> return ctx.replyError {
                    "You must specify content when creating a Custom Command!"
                }
                parts[1].length > CONTENT_MAX_LENGTH -> return ctx.replyError {
                    "Custom Command content cannot exceed 1900 characters in length!"
                }
            }

            val name = parts[0]
            val content = parts[1]

            if(ctx.guild.hasCustomCommand(name)) return ctx.replyError {
                "Custom Command named \"$name\" already exists!"
            }

            ctx.guild.setCustomCommand(name, content)
            ctx.replySuccess("Successfully created Custom Command \"**$name**\"!")
            ctx.invokeCooldown()
        }
    }

    @MustHaveArguments("Specify arguments in the format `%arguments`.")
    private inner class CustomCmdEditCommand: Command(this@CustomCmdCommand) {
        override val name = "Edit"
        override val arguments = "[Command Name] [Command Content]"
        override val help = "Edits a custom command."
        override val cooldown = 15
        override val cooldownScope = CooldownScope.USER_GUILD

        override suspend fun execute(ctx: CommandContext) {
            val parts = ctx.args.split(commandArgs, 2)

            when {
                parts[0].length > NAME_MAX_LENGTH -> return ctx.replyError {
                    "Custom Command names cannot exceed 50 characters in length!"
                }
                ctx.bot.commands[parts[0]] !== null -> return ctx.replyError {
                    "Custom Commands may not have names that match standard command names!"
                }
                parts.size == 1 -> return ctx.replyError {
                    "You must specify content when creating a Custom Command!"
                }
                parts[1].length > CONTENT_MAX_LENGTH -> return ctx.replyError {
                    "Custom Command content cannot exceed 1900 characters in length!"
                }
            }

            val name = parts[0]
            val content = parts[1]

            if(!ctx.guild.hasCustomCommand(name)) return ctx.replyError {
                "Custom Command named \"$name\" does not exist!"
            }

            ctx.guild.setCustomCommand(name, content)
            ctx.replySuccess("Successfully edited Custom Command \"**$name**\"!")
            ctx.invokeCooldown()
        }
    }

    @MustHaveArguments("Specify the name of the tag to import.")
    private inner class CustomCmdImportCommand: Command(this@CustomCmdCommand) {
        override val name = "Import"
        override val arguments = "[Tag Name]"
        override val help = "Imports a tag as a custom command."
        override val cooldown = 30
        override val cooldownScope = CooldownScope.GUILD

        override suspend fun execute(ctx: CommandContext) {
            val name = ctx.args

            if(ctx.guild.hasCustomCommand(name)) return ctx.replyError {
                "Custom Command named \"$name\" already exists!"
            }

            if(ctx.guild.isTag(name) || ctx.jda.isTag(name)) {
                val tag = checkNotNull(ctx.guild.getTagByName(name)) {
                    "Expected non-null tag from Guild (ID: ${ctx.guild.idLong}) with name $name"
                }

                ctx.guild.setCustomCommand(tag.name, tag.content)
                ctx.replySuccess("Successfully created Custom Command \"**$name**\"!")
            } else {
                ctx.replyError("Tag named \"$name\" does not exist!")
            }
        }
    }

    private inner class CustomCmdListCommand: Command(this@CustomCmdCommand) {
        override val name = "List"
        override val help = "Gets a list of all the available custom commands."
        override val cooldown = 20
        override val cooldownScope = CooldownScope.USER_GUILD
        override val defaultLevel = Level.STANDARD

        private val builder = paginatorBuilder {
            waiter           { Laxus.Waiter }
            timeout          { delay { 20 } }
            waitOnSinglePage { false }
            showPageNumbers  { true }
            numberItems      { true }
        }

        override suspend fun execute(ctx: CommandContext) {
            val commands = ctx.guild.customCommands

            if(commands.isEmpty()) {
                return ctx.replyError("There are no custom commands on this server!")
            }
            builder.clearItems()
            val paginator = paginator(builder) {
                text        { _,_ -> "Custom Commands on ${ctx.guild.name}" }
                items       { + commands.map { it.first } }
                finalAction { ctx.linkMessage(it) }
                user        { ctx.author }
            }
            paginator.displayIn(ctx.channel)
        }
    }

    @MustHaveArguments("Specify a custom command to remove.")
    private inner class CustomCmdRemoveCommand: Command(this@CustomCmdCommand) {
        override val name = "Remove"
        override val arguments = "[Command Name]"
        override val help = "Removes a custom command."

        override suspend fun execute(ctx: CommandContext) {
            val name = ctx.args

            if(!ctx.guild.hasCustomCommand(name)) return ctx.replyError {
                "Custom Command named \"$name\" does not exist!"
            }

            ctx.guild.setCustomCommand(name, null)
            ctx.replySuccess("Successfully created Custom Command \"**$name**\"!")
        }
    }
}