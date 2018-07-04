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
@file:Suppress("LiftReturnOrAssignment")
package xyz.laxus.command.standard

import net.dv8tion.jda.core.Permission.*
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.Experiment
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.db.entities.Reminder
import xyz.laxus.jda.menus.paginator
import xyz.laxus.jda.menus.paginatorBuilder
import xyz.laxus.util.Emojis
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.addReminder
import xyz.laxus.util.db.reminders
import xyz.laxus.util.db.removeAllReminders
import xyz.laxus.util.db.removeReminder
import xyz.laxus.util.parseTimeArgument
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Experiment("Reminders are experimental!")
@MustHaveArguments("Specify a time argument followed by a reminder.")
class ReminderCommand: Command(StandardGroup) {
    override val name = "Reminder"
    override val aliases = arrayOf("Remind")
    override val arguments = "[Time] [Reminder]"
    override val help = "Sets a reminder to be sent to your DM at a specific time."
    override val guildOnly = false
    override val cooldown = 20
    override val cooldownScope = CooldownScope.USER
    override val children = arrayOf(ReminderListCommand(), ReminderRemoveCommand())

    override suspend fun execute(ctx: CommandContext) {
        val splitArgs = ctx.args.split(commandArgs)
        var number = true
        var timeString = ""
        var reminderString = ctx.args
        for(arg in splitArgs) {
            if(number) {
                if(arg.toIntOrNull() === null && !arg[0].isDigit()) break
                number = false
            } else {
                if(arg.any { it.isDigit() }) break
                number = true
            }
            timeString += "$arg "
            reminderString = reminderString.removePrefix(arg).trimStart()
        }

        timeString = timeString.trim()

        if(timeString.isEmpty()) return ctx.replyError("Could not parse time from \"${ctx.args}\"!")
        if(reminderString.isEmpty()) return ctx.replyError("Cannot set an empty reminder!")
        if(reminderString.length > Reminder.MaxMessageLength) return ctx.replyError {
            "Cannot set a reminder longer than ${Reminder.MaxMessageLength} characters!"
        }

        val now = LocalDateTime.now()
        val time = parseTimeArgument(timeString, now) ?: return ctx.replyError {
            "Could not parse time argument: \"$timeString\""
        }

        ctx.author.addReminder(time.addTo(now), reminderString)
        ctx.replySuccess("Reminder set for $timeString!")
        ctx.bot.updateReminderContext()
    }

    private inner class ReminderListCommand: Command(this@ReminderCommand) {
        override val name = "List"
        override val help = "Lists your active reminders."
        override val cooldown = 20
        override val cooldownScope = CooldownScope.USER
        override val guildOnly = false
        override val botPermissions = arrayOf(
            MESSAGE_EMBED_LINKS,
            MESSAGE_MANAGE,
            MESSAGE_ADD_REACTION
        )

        private val builder = paginatorBuilder {
            waiter { Laxus.Waiter }
            waitOnSinglePage { false }
            numberItems { true }
            itemsPerPage { 5 }
            allowTextInput { false }
            showPageNumbers { false }
        }

        override suspend fun execute(ctx: CommandContext) {
            val reminders = ctx.author.reminders

            if(reminders.isEmpty()) return ctx.replyError("You have no reminders!")

            builder.clearItems()
            val paginator = paginator(builder) {
                text { p, t -> "${Emojis.AlarmClock} ${reminders.size} Reminders For **${ctx.author.name}** (Page $p/$t)" }
                color { _, _ -> ctx.takeIf { it.isGuild }?.member?.color }

                for(reminder in reminders) {
                    add { "${reminder.message} `[${formatReminderTime(reminder)}]`" }
                }

                footer { _, _ -> "Next reminder" }
                time { _, _ -> OffsetDateTime.from(reminders[0].remindTime.toInstant().atZone(ZoneOffset.UTC)) }
                finalAction { message ->
                    ctx.linkMessage(message)
                    message.guild?.let {
                        if(it.selfMember.hasPermission(message.textChannel, MESSAGE_MANAGE)) {
                            message.clearReactions().queue()
                        }
                    }
                }
            }

            paginator.displayIn(ctx.channel)
        }
    }

    @MustHaveArguments("Specify a reminder number to remove!")
    private inner class ReminderRemoveCommand: Command(this@ReminderCommand) {
        override val name = "Remove"
        override val arguments = "[Number|All]"
        override val help = "Removes a reminder from your list."
        override val guildOnly = false

        override suspend fun execute(ctx: CommandContext) {
            val reminders = ctx.author.reminders
            if(reminders.isEmpty()) return ctx.replyError {
                "You have no reminders to remove!"
            }

            val args = ctx.args
            if(args.equals("all", ignoreCase = true)) {
                ctx.author.removeAllReminders()
                ctx.replySuccess("Successfully removed all reminders!")
            } else {
                val index = args.toIntOrNull() ?: return ctx.replyError("\"$args\" was not a number!")
                val reminder = reminders.getOrNull(index - 1) ?: return ctx.replyError {
                    "`$index` is not a valid reminder number!"
                }
                ctx.author.removeReminder(reminder)
                ctx.replySuccess("Successfully removed reminder `$index`: ${reminder.message}")
            }
        }
    }

    private companion object {
        private val displayedUnits = arrayOf(
            ChronoUnit.YEARS,
            ChronoUnit.MONTHS,
            ChronoUnit.DAYS,
            ChronoUnit.HOURS,
            ChronoUnit.MINUTES,
            ChronoUnit.SECONDS
        )

        private fun formatReminderTime(reminder: Reminder): String = buildString {
            var passes = 0
            val now = LocalDateTime.now()
            var until = reminder.remindTime.toLocalDateTime()
            for(unit in displayedUnits) {
                if(passes >= 3) break
                val amount = unit.between(now, until)
                if(amount > 0) {
                    until = until.minus(amount, unit)
                    append("$amount $unit ")
                    passes++
                }
            }
        }.trim()
    }
}