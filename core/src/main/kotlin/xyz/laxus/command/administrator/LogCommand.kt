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

import net.dv8tion.jda.core.entities.TextChannel
import xyz.laxus.command.*
import xyz.laxus.db.entities.StarSettings
import xyz.laxus.entities.starboard.hasStarboard
import xyz.laxus.entities.starboard.starboardChannel
import xyz.laxus.entities.starboard.starboardSettings
import xyz.laxus.jda.util.findTextChannels
import xyz.laxus.util.db.hasModLog
import xyz.laxus.util.db.modLog
import xyz.laxus.util.multipleTextChannels
import xyz.laxus.util.noMatch
import xyz.laxus.util.titleName

/**
 * @author Kaidan Gustave
 */
class LogCommand: EmptyCommand(AdministratorGroup) {
    override val name = "Log"
    override val help = "Manage various types of server logs."
    override val children = arrayOf(
        LogSetCommand(),
        LogRemoveCommand()
    )

    @MustHaveArguments("Specify a type of log and a channel to use as it.")
    private inner class LogSetCommand : Command(this@LogCommand) {
        override val name = "Set"
        override val arguments = "[Type] [Channel]"
        override val help = "Sets the specified type of log for the server."

        override suspend fun execute(ctx: CommandContext) {
            val type = Type.fromArgs(ctx.args) ?: return ctx.error("Invalid Type") {
                "For a list of all available log types, use `${ctx.bot.prefix}${parent!!.name} types`!"
            }

            val args = type.trimArgs(ctx.args)
            val channels = ctx.guild.findTextChannels(args)

            val found = when {
                channels.isEmpty() -> return ctx.replyError(noMatch("text channels", args))
                channels.size > 1 -> return ctx.replyError(channels.multipleTextChannels(args))
                else -> channels[0]
            }

            val currentLog = type.get(ctx)

            if(currentLog == found) return ctx.replyError {
                "**${currentLog.name}** is already the ${type.titleName} for this server!"
            }

            type.set(ctx, found)
        }
    }

    @MustHaveArguments
    private inner class LogRemoveCommand: Command(this@LogCommand) {
        override val name = "Remove"
        override val arguments = "[Type]"
        override val help = "Removes the specified type of log from the server."

        override suspend fun execute(ctx: CommandContext) {
            val type = Type.fromArgs(ctx.args) ?: return ctx.error("Invalid Type") {
                "For a list of all available log types, use `${ctx.bot.prefix}${parent!!.name} types`!"
            }

            type.remove(ctx)
        }
    }

    private enum class Type(vararg val names: String): LogTypeCommandHandler {
        MOD_LOG("moderation log", "moderation", "mod log", "mod") {
            override fun get(ctx: CommandContext): TextChannel? = ctx.guild.modLog

            override fun set(ctx: CommandContext, channel: TextChannel) {
                ctx.guild.modLog = channel
                ctx.replySuccess("Successfully set ${channel.asMention} as this server's moderation log!")
            }

            override fun remove(ctx: CommandContext) {
                if(!ctx.guild.hasModLog) return ctx.replyError {
                    "This server has no moderation log to remove!"
                }

                ctx.guild.modLog = null
                ctx.replySuccess("Successfully removed this server's moderation log!")
            }
        },

        STARBOARD("starboard", "star") {
            override fun get(ctx: CommandContext): TextChannel? = ctx.guild.starboardChannel

            override fun set(ctx: CommandContext, channel: TextChannel) {
                val settings = ctx.guild.starboardSettings?.also { it.channelId = channel.idLong }
                               ?: StarSettings(channel.guild.idLong, channel.idLong)
                ctx.guild.starboardSettings = settings
                ctx.replySuccess("Successfully set ${channel.asMention} as this server's starboard!")
            }

            override fun remove(ctx: CommandContext) {
                if(!ctx.guild.hasStarboard) return ctx.replyError {
                    "This server has no starboard to remove!"
                }

                ctx.guild.starboardSettings = null
                ctx.replySuccess("Successfully removed this server's starboard!")
            }
        };

        fun trimArgs(args: String): String {
            return names.firstOrNull {
                args.startsWith(it, true)
            }?.let {
                args.substring(it.length).trim()
            } ?: args
        }

        companion object {
            fun fromArgs(args: String): Type? {
                return values().firstOrNull {
                    it.names.any {
                        args.startsWith(it, true)
                    }
                }
            }
        }
    }

    private interface LogTypeCommandHandler {
        fun get(ctx: CommandContext): TextChannel?

        fun configure(ctx: CommandContext) {}

        fun set(ctx: CommandContext, channel: TextChannel)

        fun remove(ctx: CommandContext)
    }
}
