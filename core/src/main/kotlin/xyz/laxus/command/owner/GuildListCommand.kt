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

import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.db.DBGuilds
import xyz.laxus.db.DBGuilds.Type.MUSIC
import xyz.laxus.jda.menus.paginator
import xyz.laxus.jda.menus.paginatorBuilder
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.isMusic
import xyz.laxus.util.titleName

/**
 * @author Kaidan Gustave
 */
class GuildListCommand: Command(OwnerGroup) {
    override val name = "GuildList"
    override val help = "Gets a list of all the guilds the bot is on."
    override val guildOnly = false
    override val hasAdjustableLevel = false
    override val children = arrayOf(
        GuildListAddCommand(),
        GuildListRemoveCommand()
    )

    private val builder = paginatorBuilder {
        timeout          { delay { 20 } }
        waiter           { Laxus.Waiter }
        waitOnSinglePage { true }
        numberItems      { true }
        itemsPerPage     { 10 }
        text             { t, u -> "Page $t/$u" }
        finalAction      { it.delete().queue() }
    }

    override suspend fun execute(ctx: CommandContext) {
        builder.clearItems()
        val paginator = paginator(builder) {
            ctx.jda.guilds.forEach { + "**${it.name}** (ID: ${it.id})" }
            if(ctx.isGuild) {
                color { _, _ -> ctx.member.color }
            }
            user { ctx.author }
        }
        paginator.displayIn(ctx.channel)
    }

    @MustHaveArguments("Specify a listing category and a Guild ID.")
    private inner class GuildListAddCommand: Command(this@GuildListCommand) {
        override val name = "Add"
        override val arguments = "[Category] <Guild ID>"
        override val help = "Adds a guild to a listing category."
        override val hasAdjustableLevel = false

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            val splitArgs = args.split(commandArgs, 2)

            val type: DBGuilds.Type
            val guildId: Long

            if(splitArgs.size == 2) {
                val typeSpecified = splitArgs[0].toUpperCase()
                type = DBGuilds.Type.values().find { it.name == typeSpecified } ?: return ctx.replyError {
                    "\"$typeSpecified\" is not a valid guild-list type!"
                }
                guildId = try { splitArgs[1].toLong() } catch(e: NumberFormatException) {
                    return ctx.replyError {
                        "Invalid Guild ID: `${splitArgs[1]}`"
                    }
                }
            } else {
                if(!ctx.isGuild) return ctx.replyError {
                    "You must be in a guild to use $fullname without providing a guild ID."
                }
                val typeSpecified = splitArgs[0].toUpperCase()
                type = DBGuilds.Type.values().find { it.name == typeSpecified } ?: return ctx.replyError {
                    "\"$typeSpecified\" is not a valid guild-list type!"
                }
                guildId = ctx.guild.idLong
            }

            val guild = ctx.jda.getGuildById(guildId) ?: return ctx.replyError {
                "Could not find a guild with ID: `$guildId`!"
            }

            when(type) {
                MUSIC -> {
                    if(guild.isMusic) return ctx.replyError {
                        "**${guild.name}** is already listed for **Music**!"
                    }
                    guild.isMusic = true
                }

                else -> return ctx.replyWarning("Type **${type.titleName}** is not yet supported!")
            }

            ctx.replySuccess("Successfully added **${guild.name}** as **${type.titleName}**!")
        }
    }

    @MustHaveArguments("Specify a listing category and a Guild ID.")
    private inner class GuildListRemoveCommand: Command(this@GuildListCommand) {
        override val name = "Remove"
        override val arguments = "[Category] <Guild ID>"
        override val help = "Removes a guild to a listing category."
        override val hasAdjustableLevel = false

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args
            val splitArgs = args.split(commandArgs, 2)

            val type: DBGuilds.Type
            val guildId: Long

            if(splitArgs.size == 2) {
                val typeSpecified = splitArgs[0].toUpperCase()
                type = DBGuilds.Type.values().find { it.name == typeSpecified } ?: return ctx.replyError {
                    "\"$typeSpecified\" is not a valid guild-list type!"
                }
                guildId = try { splitArgs[1].toLong() } catch(e: NumberFormatException) {
                    return ctx.replyError {
                        "Invalid Guild ID: `${splitArgs[1]}`"
                    }
                }
            } else {
                if(!ctx.isGuild) return ctx.replyError {
                    "You must be in a guild to use $fullname without providing a guild ID."
                }
                val typeSpecified = splitArgs[0].toUpperCase()
                type = DBGuilds.Type.values().find { it.name == typeSpecified } ?: return ctx.replyError {
                    "\"$typeSpecified\" is not a valid guild-list type!"
                }
                guildId = ctx.guild.idLong
            }

            val guild = ctx.jda.getGuildById(guildId) ?: return ctx.replyError {
                "Could not find a guild with ID: `$guildId`!"
            }

            when(type) {
                MUSIC -> {
                    if(!guild.isMusic) return ctx.replyError {
                        "**${guild.name}** is not listed for **Music**!"
                    }
                    guild.isMusic = false
                }

                else -> return ctx.replyWarning("Type **${type.titleName}** is not yet supported!")
            }

            ctx.replySuccess("Successfully removed **${guild.name}** as **${type.titleName}**!")
        }
    }
}