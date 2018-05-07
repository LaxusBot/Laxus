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
import xyz.laxus.jda.menus.paginator
import xyz.laxus.jda.menus.paginatorBuilder
import xyz.laxus.util.db.addPrefix
import xyz.laxus.util.db.hasPrefix
import xyz.laxus.util.db.prefixes
import xyz.laxus.util.db.removePrefix

/**
 * @author Kaidan Gustave
 */
class PrefixCommand: EmptyCommand(AdministratorGroup) {
    override val name = "Prefix"
    override val help = "Manage the bot's custom prefixes for the server."
    override val children = arrayOf(
        PrefixAddCommand(),
        PrefixListCommand(),
        PrefixRemoveCommand()
    )

    @MustHaveArguments("Specify a prefix to add!")
    private inner class PrefixAddCommand: Command(this@PrefixCommand) {
        override val name = "Add"
        override val arguments = "[Prefix]"
        override val help = "Adds a custom prefix to the bot for the server."

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            when {
                args.equals(ctx.bot.prefix, true) -> return ctx.replyError {
                    "`$args` cannot be added as a prefix because it is the default prefix!"
                }
                args.length > 50 -> return ctx.replyError {
                    "`$args` cannot be added as a prefix because it is the default prefix!"
                }
                ctx.guild.hasPrefix(args) -> return ctx.replyError {
                    "`$args` cannot be added as a prefix because it is already a prefix!"
                }
                else -> {
                    ctx.guild.addPrefix(args)
                    ctx.replySuccess("`$args` was added as a prefix!")
                }
            }
        }
    }

    @MustHaveArguments("Specify a prefix to remove!")
    private inner class PrefixRemoveCommand: Command(this@PrefixCommand) {
        override val name = "Remove"
        override val arguments = "[Prefix]"
        override val help = "Removes a custom prefix from the bot for the server."

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            when {
                args.equals(ctx.bot.prefix, true) -> return ctx.replyError {
                    "`$args` cannot be removed as a prefix because it is the default prefix!"
                }
                !ctx.guild.hasPrefix(args) -> return ctx.replyError {
                    "`$args` cannot be removed as a prefix because it is not a prefix!"
                }
                else -> {
                    ctx.guild.removePrefix(args)
                    ctx.replySuccess("`$args` was removed as a prefix!")
                }
            }
        }
    }

    private inner class PrefixListCommand: Command(this@PrefixCommand) {
        override val name = "List"
        override val help = "Gets a list of the server's custom prefixes."
        override val cooldown = 10
        override val cooldownScope = CooldownScope.USER_GUILD
        override val defaultLevel = Level.STANDARD

        private val builder = paginatorBuilder {
            waiter           { Laxus.Waiter }
            waitOnSinglePage { false }
            showPageNumbers  { true }
            itemsPerPage     { 10 }
            numberItems      { true }
            text             { p, t -> "Server Prefixes${if(t > 1) " [`$p/$t`]" else ""}" }
        }

        override suspend fun execute(ctx: CommandContext) {
            val prefixes = ctx.guild.prefixes
            if(prefixes.isEmpty()) return ctx.replyWarning {
                "This server has no custom prefixes!"
            }
            builder.clearItems()
            val paginator = paginator(builder) {
                items { + prefixes }
                finalAction { ctx.linkMessage(it) }
                user  { ctx.author }
            }
            paginator.displayIn(ctx.channel)
        }
    }
}