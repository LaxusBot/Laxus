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

import net.dv8tion.jda.core.entities.User
import xyz.laxus.db.DBReminders
import xyz.laxus.db.entities.Reminder
import java.time.temporal.Temporal

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