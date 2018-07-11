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
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.findMembers
import xyz.laxus.util.*
import xyz.laxus.util.db.timezone
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class TimeCommand: Command(StandardGroup) {
    override val name = "Time"
    override val aliases = arrayOf("TF", "TimeFor")
    override val arguments = "<User>"
    override val help = "Gets the time for a user if they have set it."
    override val guildOnly = false
    override val children: Array<Command> = arrayOf(TimeZoneCommand())

    override suspend fun execute(ctx: CommandContext) {
        val user = if(ctx.args.isEmpty()) ctx.author else {
            if(!ctx.isGuild) return ctx.replyError {
                "Specifying a user for this command is only available in servers!"
            }

            val found = ctx.guild.findMembers(ctx.args)
            when {
                found.isEmpty() -> return ctx.replyError(noMatch("members", ctx.args))
                found.size > 1 -> return ctx.replyError(found.multipleMembers(ctx.args))
                else -> found[0].user
            }
        }

        val timezone = user.timezone ?: return ctx.replyError {
            if(ctx.args.isNotEmpty()) {
                "${user.formattedName(true)} has not set their timezone!"
            } else {
                "You have not set your timezone!\n" +
                "Use `${ctx.bot.prefix}${this.fullname} Zone Set` to set your timezone!"
            }
        }

        val time = OffsetDateTime.now(timezone.toZoneId())

        val hour = time.hour
        val minute = time.minute
        val halfOfDay = if(hour in 12..23) "PM" else "AM"
        val military = "`${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`"
        val t = buildString {
            append('`')
            when {
                hour == 0 -> append(12)
                hour > 12 -> append(hour % 12)
                else -> append(hour)
            }
            append(':')
            append(minute.toString().padStart(2, '0'))
            append(halfOfDay)
            append('`')
            append(" ($military)")
        }

        if(ctx.args.isEmpty()) {
            ctx.reply("${Emojis.AlarmClock} The current time is $t.")
        } else {
            ctx.reply("${Emojis.AlarmClock} The current time for ${user.formattedName(true)} is $t.")
        }
    }

    private inner class TimeZoneCommand: Command(this@TimeCommand) {
        override val name = "Zone"
        override val arguments = "<User>"
        override val help = "Gets the timezone for a user if they have set it."
        override val guildOnly = false
        override val children = arrayOf(
            TimeZoneSetCommand(),
            TimeZoneUnsetCommand()
        )

        override suspend fun execute(ctx: CommandContext) {
            val user = if(ctx.args.isEmpty()) ctx.author else {
                if(!ctx.isGuild) return ctx.replyError {
                    "Specifying a user for this command is only available in servers!"
                }

                val found = ctx.guild.findMembers(ctx.args)
                when {
                    found.isEmpty() -> return ctx.replyError(noMatch("members", ctx.args))
                    found.size > 1 -> return ctx.replyError(found.multipleMembers(ctx.args))
                    else -> found[0].user
                }
            }

            val timezone = user.timezone ?: return ctx.replyError {
                if(ctx.args.isNotEmpty()) {
                    "${user.formattedName(true)} has not set their timezone!"
                } else {
                    "You have not set your timezone!\n" +
                    "Use `${ctx.bot.prefix}${this.fullname} Set` to set your timezone!"
                }
            }

            if(ctx.args.isNotEmpty()) {
                ctx.reply("${Emojis.GlobeWithMeridians} ${user.name}'s timezone is **${timezone.id}**.")
            } else {
                ctx.reply("${Emojis.GlobeWithMeridians} Your timezone is **${timezone.id}**.")
            }
        }

        @MustHaveArguments("Specify your timezone!")
        private inner class TimeZoneSetCommand: Command(this@TimeZoneCommand) {
            override val name = "Set"
            override val arguments = "[TimeZone|ZoneId]"
            override val help = "Sets your timezone."
            override val cooldown = 20
            override val cooldownScope = CooldownScope.USER
            override val guildOnly = false

            override suspend fun execute(ctx: CommandContext) {
                val args = ctx.args
                val timezoneId = availableZones[args.toLowerCase()]

                val timezone = when(timezoneId) {
                    null -> {
                        val offset = args.replace(" ", "").toIntOrNull() ?: return ctx.replyError {
                            "\"$args\" is not a valid time zone!"
                        }

                        if(offset !in -18..+18) return ctx.replyError {
                            "Invalid offset! Offset must be a number between `-18` and `+18`!"
                        }

                        TimeZone.getTimeZone(ZoneOffset.ofHours(offset))
                    }

                    else -> TimeZone.getTimeZone(timezoneId)
                }

                ctx.author.timezone = timezone
                ctx.replySuccess("Successfully set timezone to **${timezone.id}**!")
            }
        }

        private inner class TimeZoneUnsetCommand: Command(this@TimeZoneCommand) {
            override val name = "Unset"
            override val help = "Unsets your timezone."
            override val cooldown = 20
            override val cooldownScope = CooldownScope.USER
            override val guildOnly = false

            override suspend fun execute(ctx: CommandContext) {
                val timezone = ctx.author.timezone
                if(timezone === null) return ctx.replyError {
                    "TimeZone cannot be unset because it hasn't been set to begin with!"
                }

                ctx.author.timezone = null
                ctx.replySuccess("Successfully unset timezone from **${timezone.id}**")
            }
        }
    }

    private companion object {
        private val availableZones = TimeZone.getAvailableIDs().associate { it.toLowerCase() to it }
    }
}
