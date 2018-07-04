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

import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.db.entities.Reminder
import xyz.laxus.jda.util.embed
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.addReminder
import xyz.laxus.util.db.reminders
import xyz.laxus.util.formattedName
import xyz.laxus.util.parseTimeArgument
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@MustHaveArguments("Specify a time argument followed by a reminder.")
class ReminderCommand: Command(StandardGroup) {
    override val name = "Reminder"
    override val aliases = arrayOf("Remind")
    override val arguments = "[Time] [Reminder]"
    override val help = "Sets a reminder."
    override val guildOnly = false
    override val cooldown = 20
    override val cooldownScope = CooldownScope.USER
    override val children = arrayOf(ReminderListCommand())

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

        val now = LocalDateTime.now()
        val time = parseTimeArgument(timeString, now) ?: return ctx.replyError {
            "Could not parse time argument: \"$timeString\""
        }

        ctx.author.addReminder(time.addTo(now), reminderString)
        ctx.replySuccess("Reminder set for $timeString!")
    }

    inner class ReminderListCommand: Command(this@ReminderCommand) {
        override val name = "List"
        override val help = "Lists your active reminders"
        override val cooldown = 5
        override val cooldownScope = CooldownScope.USER
        override val guildOnly = false

        override suspend fun execute(ctx: CommandContext) {
            val reminders = ctx.author.reminders

            if(reminders.isEmpty()) return ctx.replyError("You have no reminders!")

            val embed = embed {
                author {
                    value { "Reminders for ${ctx.author.formattedName()}" }
                    icon { ctx.author.effectiveAvatarUrl }
                }

                val max = 500 / reminders.size
                for((i, reminder) in reminders.withIndex()) {
                    val m = if(reminder.message.length > max) reminder.message.substring(0, max) else reminder.message
                    append("`${i + 1}` **$m** `[${formatReminderTime(reminder)}]`")
                    if(i < reminders.lastIndex) appendln()
                }

                color { ctx.takeIf { it.isGuild }?.member?.color }

                if(reminders.size > 1) {
                    footer { value { "Next reminder" } }
                    time { OffsetDateTime.from(reminders[0].remindTime.toInstant().atZone(ZoneOffset.UTC)) }
                }
            }

            ctx.reply(embed)
        }
    }

    private fun formatReminderTime(reminder: Reminder): String = buildString {
        var until = reminder.remindTime.toLocalDateTime()
        val now = LocalDateTime.now()
        val years = ChronoUnit.YEARS.between(now, until)
        until = if(years == 0L) until else {
            append("$years years ")
            until.minusYears(years)
        }
        val months = ChronoUnit.MONTHS.between(now, until)
        until = if(months == 0L) until else {
            append("$months months ")
            until.minusMonths(months)
        }
        val days = ChronoUnit.DAYS.between(now, until)
        until = if(days == 0L) until else {
            append("$days days ")
            until.minusDays(days)
        }
        val hours = ChronoUnit.HOURS.between(now, until)
        until = if(hours == 0L) until else {
            append("$hours hours ")
            until.minusHours(hours)
        }
        val minutes = ChronoUnit.MINUTES.between(now, until)
        until = if(minutes == 0L) until else {
            append("$minutes minutes ")
            until.minusMinutes(minutes)
        }
        val seconds = ChronoUnit.SECONDS.between(now, until)
        if(seconds > 0L) append("$seconds seconds ")
    }.trim()
}