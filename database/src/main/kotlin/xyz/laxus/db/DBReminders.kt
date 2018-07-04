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
package xyz.laxus.db

import xyz.laxus.db.entities.Reminder
import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.UPDATABLE
import xyz.laxus.db.sql.ResultSetType.SCROLL_INSENSITIVE

@TableName("reminders")
@Columns(
    Column("id", "BIGSERIAL", primary = true),
    Column("user_id", BIGINT, primary = true),
    Column("remind_time", TIMESTAMP),
    Column("message", "$VARCHAR(${Reminder.MaxMessageLength})")
)
object DBReminders: Table() {
    fun nextReminder(): Reminder? {
        connection.prepare("SELECT * FROM reminders ORDER BY remind_time ASC") {
            it.executeQuery {
                if(it.next()) {
                    return Reminder(it)
                }
            }
        }
        return null
    }

    fun addReminder(reminder: Reminder) {
        require(reminder.id == -1L) { "Invalid Reminder.id for adding!" }
        connection.prepare("SELECT * FROM reminders WHERE user_id = ? ORDER BY remind_time ASC",
            SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = reminder.userId
            statement.executeQuery {
                it.insert {
                    it["user_id"] = reminder.userId
                    it["remind_time"] = reminder.remindTime
                    it["message"] = reminder.message
                }
            }
        }
    }

    fun getReminders(userId: Long): List<Reminder> {
        val list = arrayListOf<Reminder>()
        connection.prepare("SELECT * FROM reminders WHERE user_id = ? ORDER BY remind_time ASC") { statement ->
            statement[1] = userId
            statement.executeQuery {
                it.whileNext {
                    list += Reminder(it)
                }
            }
        }
        return list
    }

    fun removeReminder(reminder: Reminder) {
        require(reminder.id != -1L) { "Invalid Reminder.id for removal!" }
        connection.prepare("SELECT * FROM reminders WHERE id = ?",
            SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = reminder.id
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeAllReminders(userId: Long) {
        connection.prepare("SELECT * FROM reminders WHERE user_id = ?",
            SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = userId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }
}