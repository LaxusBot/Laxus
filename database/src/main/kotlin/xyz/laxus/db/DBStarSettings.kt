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

import xyz.laxus.db.annotation.Column
import xyz.laxus.db.annotation.Columns
import xyz.laxus.db.annotation.TableName
import xyz.laxus.db.entities.StarSettings
import xyz.laxus.db.sql.*
import xyz.laxus.util.checkInRange

/**
 * @author Kaidan Gustave
 */
@TableName("star_settings")
@Columns(
    Column("guild_id", "BIGINT", primary = true),
    Column("channel_id", "BIGINT"),
    Column("threshold", "SMALLINT", def = "5"),
    Column("max_age", "INT", def = "72"),
    Column("ignored", "BIGINT[]", def = "ARRAY[]::BIGINT[]")
)
object DBStarSettings: Table() {
    fun hasSettings(guildId: Long): Boolean {
        return connection.prepare("SELECT * FROM star_settings WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery { it.next() }
        }
    }

    fun getSettings(guildId: Long): StarSettings? {
        connection.prepare("SELECT * FROM star_settings WHERE guild_id = ?") { statement ->
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
        connection.update("SELECT * FROM star_settings WHERE guild_id = ?") { statement ->
            statement[1] = settings.guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = settings.guildId
                    it["channel_id"] = settings.channelId
                    it["threshold"] = settings.threshold
                    it["max_age"] = settings.maxAge
                    it["ignored"] = settings.ignored.toLongArray()
                } else it.update {
                    it["channel_id"] = settings.channelId
                    it["threshold"] = settings.threshold
                    it["max_age"] = settings.maxAge
                    it["ignored"] = settings.ignored.toLongArray()
                }
            }
        }
    }

    fun removeSettings(guildId: Long) {
        connection.update("SELECT * FROM star_settings WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }
}