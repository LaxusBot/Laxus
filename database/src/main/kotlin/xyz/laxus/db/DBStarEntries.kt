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

import xyz.laxus.db.schema.BIGINT
import xyz.laxus.db.schema.Column
import xyz.laxus.db.schema.Columns
import xyz.laxus.db.schema.TableName
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*

/**
 * @author Kaidan Gustave
 */
@TableName("star_entries")
@Columns(
    Column("starred_id", BIGINT, primary = true),
    Column("entry_id", BIGINT, nullable = true, def = "NULL"),
    Column("starboard_id", BIGINT),
    Column("guild_id", BIGINT, primary = true),
    Column("user_id", BIGINT, primary = true)
)
object DBStarEntries : Table() {
    fun addStar(starredId: Long, guildId: Long, userId: Long, entryId: Long? = null) {
        val starboardId = checkNotNull(DBStarSettings.getSettings(guildId)?.channelId) {
            "No StarSettings found for Guild ID: $guildId"
        }

        connection.prepare("SELECT * FROM star_entries WHERE starred_id = ? AND guild_id = ? AND user_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement[3] = userId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["starred_id"] = starredId
                    it["entry_id"] = entryId
                    it["starboard_id"] = starboardId
                    it["guild_id"] = guildId
                    it["user_id"] = userId
                }
            }
        }

        entryId?.let { setEntry(entryId, starredId, guildId) }
    }

    fun getStars(starredId: Long, guildId: Long): List<Long> {
        val stars = ArrayList<Long>()
        connection.prepare("SELECT USER_ID FROM star_entries WHERE starred_id = ? AND guild_id = ?") { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement.executeQuery {
                it.whileNext { stars += it.getLong("user_id") }
            }
        }
        return stars
    }

    fun isStarring(starredId: Long, guildId: Long, userId: Long): Boolean {
        return connection.prepare("SELECT * FROM star_entries WHERE starred_id = ? AND guild_id = ? AND user_id = ?") { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement[3] = userId
            statement.executeQuery { it.next() }
        }
    }

    fun getStarCount(starredId: Long, guildId: Long): Int {
        connection.prepare("SELECT COUNT(*) FROM star_entries WHERE starred_id = ? AND guild_id = ?") { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return it.getInt("COUNT(*)")
                }
            }
        }
        return 0
    }

    fun removeEntry(userId: Long, starredId: Long, guildId: Long) {
        connection.prepare("SELECT * FROM star_entries WHERE starred_id = ? AND guild_id = ? AND user_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = userId
            statement[2] = starredId
            statement[3] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeAllEntries(starredId: Long, guildId: Long) {
        connection.prepare("SELECT * FROM star_entries WHERE starred_id = ? AND guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }

    fun setEntry(entryId: Long, starredId: Long, guildId: Long) {
        connection.prepare("SELECT * FROM star_entries WHERE starred_id = ? AND guild_id = ? AND entry_id IS NOT NULL", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement.executeQuery {
                it.whileNext {
                    it.update { it["entry_id"] = entryId }
                }
            }
        }
    }
}