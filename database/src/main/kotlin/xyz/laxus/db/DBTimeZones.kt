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

import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*
import java.time.ZoneId
import java.util.*

@TableName("timezones")
@Columns(
    Column("user_id", BIGINT, primary = true),
    Column("user_timezone", "$VARCHAR(100)")
)
object DBTimeZones: Table() {
    fun getTimeZone(userId: Long): TimeZone? {
        connection.prepare("SELECT * FROM timezones WHERE user_id = ?") { statement ->
            statement[1] = userId
            statement.executeQuery {
                if(it.next()) {
                    return TimeZone.getTimeZone(it.getString("user_timezone"))
                }
            }
        }
        return null
    }

    fun setTimeZone(userId: Long, timezone: TimeZone) {
        connection.prepare("SELECT * FROM timezones WHERE user_id = ?",
            SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = userId
            statement.executeQuery {
                if(it.next()) it.update {
                    it["user_timezone"] = timezone.id
                } else it.insert {
                    it["user_id"] = userId
                    it["user_timezone"] = timezone.id
                }
            }
        }
    }

    fun removeTimeZone(userId: Long) {
        connection.prepare("SELECT * FROM timezones WHERE user_id = ?",
            SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = userId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }
}