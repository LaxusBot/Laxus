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

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.TextChannel
import xyz.laxus.command.*
import xyz.laxus.db.entities.StarSettings
import xyz.laxus.entities.starboard.hasStarboard
import xyz.laxus.entities.starboard.starboard
import xyz.laxus.entities.starboard.starboardChannel
import xyz.laxus.entities.starboard.starboardSettings
import xyz.laxus.jda.util.embed
import xyz.laxus.jda.util.findTextChannels
import xyz.laxus.util.commandArgs
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
        LogConfigureCommand(),
        LogSetCommand(),
        LogRemoveCommand(),
        LogTypesCommand()
    )

    @ExperimentalCommand("Configuring logs is an experimental feature!")
    @MustHaveArguments("Specify an a log type, configuration, and, if necessary, a value.")
    private inner class LogConfigureCommand: Command(this@LogCommand) {
        override val name = "Configure"
        override val aliases = arrayOf("Config")
        override val arguments = "[Type] [Configuration] <Value>"
        override val help = "Configures a specified type of log on the server."

        override suspend fun execute(ctx: CommandContext) {
            val type = Type.fromArgs(ctx.args) ?: return ctx.error("Invalid Type") {
                "For a list of all available log types, use `${ctx.bot.prefix}${parent!!.name} types`!"
            }

            val parts = type.trimArgs(ctx.args).split(commandArgs, 2)
            if(parts.isEmpty()) return ctx.replyError {
                "Log configuration wasn't specified, try specifying a configuration to set!"
            }

            val configuration = parts[0]
            val value = parts.takeIf { it.size > 1 }?.get(1)
            type.configure(ctx, configuration, value)
        }
    }

    @MustHaveArguments("Specify a type of log and a channel to use as it.")
    private inner class LogSetCommand: Command(this@LogCommand) {
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

    @MustHaveArguments("Specify a type of log to remove from this server.")
    private inner class LogRemoveCommand: Command(this@LogCommand) {
        override val name = "Remove"
        override val arguments = "[Type]"
        override val help = "Removes the specified type of log from the server."

        override suspend fun execute(ctx: CommandContext) {
            val type = Type.fromArgs(ctx.args) ?: return ctx.error("Invalid Type") {
                "For a list of all available log types, use `${ctx.bot.prefix}${parent!!.name} types`!"
            }

            if(!type.has(ctx)) return ctx.replyError {
                "This server doesn't have a ${type.titleName}!"
            }

            type.remove(ctx)
        }
    }

    private inner class LogTypesCommand: Command(this@LogCommand) {
        override val name = "Types"
        override val help = "Gets a list of all available log types."
        override val defaultLevel get() = Level.MODERATOR

        override suspend fun execute(ctx: CommandContext) {
            val embed = embed {
                title { "__Types of logs available on **${ctx.guild.name}**__" }
                color { ctx.selfMember.color }
                val types = Type.values()
                types.forEachIndexed { i, type ->
                    field(type.titleName) {
                        appendln(type.description)
                        if(i < types.lastIndex) {
                            append(EmbedBuilder.ZERO_WIDTH_SPACE)
                        }
                    }
                }
            }

            ctx.reply(embed)
        }
    }

    ///////////
    // Types //
    ///////////

    private enum class Type(vararg val names: String): LogTypeCommandHandler {
        MOD_LOG("moderation log", "moderation", "mod log", "mod") {
            override val description =
                "Logs all moderation events, such as kicks, bans, mutes, cleans, etc."

            override fun has(ctx: CommandContext): Boolean = ctx.guild.hasModLog

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

            override fun configure(ctx: CommandContext, configuration: String, value: String?) {
                ctx.replyWarning("Moderation logs cannot be configured!")
            }
        },

        STARBOARD("starboard", "star") {
            override val description =
                "Logs starred messages in the server. Can be configured to only log messages with " +
                "a specific number of stars, or messages that are not older than a certain length of time."

            override fun has(ctx: CommandContext): Boolean = ctx.guild.hasStarboard

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

            override fun configure(ctx: CommandContext, configuration: String, value: String?) {
                val starboard = checkNotNull(ctx.guild.starboard) {
                    "Starboard was null after confirming it exists!"
                }

                when(configuration.toLowerCase()) {
                    "threshold", "minimum" -> {
                        if(value === null) return ctx.replyError {
                            "You must specify a threshold to use!"
                        }

                        val threshold = try {
                           value.toShort().takeIf { it in 3..12 } ?: return ctx.error(InvalidArguments) {
                                "Threshold must be a positive integer between 3 and 12"
                            }
                        } catch(e: NumberFormatException) {
                            return ctx.error(InvalidArguments) {
                                "Threshold must be a positive integer between 3 and 12"
                            }
                        }

                        if(threshold == starboard.threshold) return ctx.replyError {
                            "Starboard max age is already set to `$threshold`!"
                        }

                        starboard.threshold = threshold

                        ctx.replySuccess("Successfully set starboard threshold to `$threshold`!")
                    }

                    "maxage" -> {
                        if(value === null) return ctx.replyError {
                            "You must specify a maximum age to use!"
                        }

                        val maxAge = try {
                            value.toInt().takeIf { it in 6..(24 * 14) } ?: return ctx.error(InvalidArguments) {
                                "Maximum age must be a positive integer between 6 and ${24 * 14} (unit is hours)"
                            }
                        } catch(e: NumberFormatException) {
                            return ctx.error(InvalidArguments) {
                                "Maximum age must be a positive integer between 6 and ${24 * 14} (unit is hours)"
                            }
                        }

                        if(maxAge == starboard.maxAge) return ctx.replyError {
                            "Starboard max age is already set to `$maxAge`!"
                        }
                        starboard.maxAge = maxAge
                        ctx.replySuccess("Successfully set starboard max age to `$maxAge`!")
                    }

                    else -> ctx.replyError("'$configuration' is not a valid configuration option for starboard!")
                }
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
        val description: String

        fun has(ctx: CommandContext): Boolean

        fun get(ctx: CommandContext): TextChannel?

        fun configure(ctx: CommandContext, configuration: String, value: String?)

        fun set(ctx: CommandContext, channel: TextChannel)

        fun remove(ctx: CommandContext)
    }
}
