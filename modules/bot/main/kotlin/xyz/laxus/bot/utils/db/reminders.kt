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
@file:Suppress("unused")
package xyz.laxus.bot.utils.db

import kotlinx.coroutines.*
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.User
import xyz.laxus.bot.utils.Emojis
import xyz.laxus.bot.utils.jda.await
import xyz.laxus.db.DBReminders
import xyz.laxus.db.entities.Reminder
import xyz.laxus.utils.createLogger
import java.time.LocalDateTime
import java.time.temporal.Temporal
import java.util.concurrent.TimeUnit

val User.reminders: List<Reminder> get() = DBReminders.getReminders(idLong)

fun User.addReminder(time: Temporal, message: String) {
    DBReminders.addReminder(Reminder(idLong, time, message))
}

fun User.removeReminder(reminder: Reminder) {
    DBReminders.removeReminder(reminder)
}

fun User.removeAllReminders() {
    DBReminders.removeAllReminders(idLong)
}

internal class ReminderManager(private val jda: JDA): AutoCloseable {
    private companion object {
        private val log = createLogger(ReminderManager::class)
    }

    private val executionContext = newSingleThreadContext("Reminders Execution Context")
    private val jobContext = newSingleThreadContext("Reminders Job Context")
    private val job = GlobalScope.launch(jobContext) {
        while(isActive) {
            delay(5, TimeUnit.SECONDS)
            while(true) {
                val reminder = DBReminders.nextReminder() ?: break
                // because reminders are polled in order of occurrence, we break
                //if we poll one that has yet to expire
                if(reminder.remindTime.toLocalDateTime().isAfter(LocalDateTime.now())) break
                launch(executionContext) {
                    runCatching {
                        val user = jda.retrieveUserById(reminder.userId).await()
                        val channel = user.openPrivateChannel().await()
                        channel.sendMessage("${Emojis.AlarmClock} **Reminder:** ${reminder.message}").await()
                    }
                }
                DBReminders.removeReminder(reminder)
            }
        }
    }

    override fun close() {
        executionContext.close()
        jobContext.close()
        job.cancel()
    }
}
