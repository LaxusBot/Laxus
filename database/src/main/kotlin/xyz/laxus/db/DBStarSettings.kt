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

import xyz.laxus.db.entities.StarSettings
import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*
import xyz.laxus.util.checkInRange

/**
 * @author Kaidan Gustave
 */
@TableName("STAR_SETTINGS")
@Columns(
    Column("GUILD_ID", BIGINT, unique = true),
    Column("CHANNEL_ID", BIGINT),
    Column("THRESHOLD", SMALLINT, def = "5"),
    Column("MAX_AGE", INT, def = "72"),
    Column("IGNORED", "$VARCHAR(2000)", nullable = true, def = "NULL")
)
object DBStarSettings: Table() {
    fun hasSettings(guildId: Long): Boolean {
        return connection.prepare("SELECT * FROM STAR_SETTINGS WHERE GUILD_ID = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery { it.next() }
        }
    }

    fun getSettings(guildId: Long): StarSettings? {
        connection.prepare("SELECT * FROM STAR_SETTINGS WHERE GUILD_ID = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return StarSettings(it)
                }
            }
        }
        return null
    }

    fun setSettings(settings: StarSettings) {
        checkInRange(settings.threshold.toInt(), 3..12)
        checkInRange(settings.maxAge, 6..(24 * 14))
        connection.prepare("SELECT * FROM STAR_SETTINGS WHERE GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = settings.guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["GUILD_ID"] = settings.guildId
                    it["CHANNEL_ID"] = settings.channelId
                    it["THRESHOLD"] = settings.threshold
                    it["MAX_AGE"] = settings.maxAge
                    it["IGNORED"] = settings.ignored.takeIf { it.isNotEmpty() }?.joinToString("|")
                } else it.update {
                    it["CHANNEL_ID"] = settings.channelId
                    it["THRESHOLD"] = settings.threshold
                    it["MAX_AGE"] = settings.maxAge
                    it["IGNORED"] = settings.ignored.takeIf { it.isNotEmpty() }?.joinToString("|")
                }
            }
        }
    }

    fun removeSettings(guildId: Long) {
        connection.prepare("SELECT * FROM STAR_SETTINGS WHERE GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }
}