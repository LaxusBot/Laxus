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
package xyz.laxus.util.db

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.User
import org.slf4j.Logger
import xyz.laxus.db.DBReminders
import xyz.laxus.db.entities.Reminder
import xyz.laxus.jda.util.await
import xyz.laxus.util.Emojis
import xyz.laxus.util.createLogger
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
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

internal class ReminderManager(private val jda: JDA) {
    private companion object Log: Logger by createLogger(ReminderManager::class)

    @Volatile private var reminderJob: Job? = null

    private val remindersContext = newSingleThreadContext("Reminders Context")
    private val lock = Mutex(locked = false)

    suspend fun update() {
        lock.withLock(owner = this) {
            this.reminderJob?.cancel()
            this.reminderJob = launch(remindersContext, onCompletion = {
                if(it is CancellationException) {
                    return@launch Log.debug("Received cancellation: ${it.message ?: "no reason"}")
                }
                it?.let { Log.warn("Reminder job was cancelled due to an unexpected error: ", it) }
            }, block = {
                while(this.isActive) {
                    val reminder = DBReminders.nextReminder() ?: break // We have no reminders in the DB, end the job
                    val channel = try {
                        jda.retrieveUserById(reminder.userId).await()
                            .openPrivateChannel().await()
                    } catch(t: Throwable) {
                        if(t is CancellationException) throw t
                        // We couldn't get the user, this is probably because
                        //they don't exist anymore, so we will skip the reminder
                        // It might also be that the user has blocked us, or we
                        //just can't DM them for some other reason
                        DBReminders.removeReminder(reminder)
                        continue
                    }

                    val now = LocalDateTime.now()
                    val then = reminder.remindTime.toLocalDateTime()

                    // Only delay if we need to wait!
                    if(now.isBefore(then)) {
                        val millis = ChronoUnit.MILLIS.between(now, then)
                        delay(millis, TimeUnit.MILLISECONDS)
                    }
                    // Remove the reminder from the DB, there should be
                    //no reason to save it after this.
                    DBReminders.removeReminder(reminder)

                    try {
                        channel.sendMessage("${Emojis.AlarmClock} **Reminder:** ${reminder.message}").queue({}, {})
                    } catch(e: Exception) {
                        Log.warn("Unexpected exception while sending message to user with ID: ${channel.user.idLong}")
                    }
                }
            })
        }
    }

    fun close(cancellation: CancellationException? = null) {
        remindersContext.cancel(cancellation)
        remindersContext.close()
    }
}