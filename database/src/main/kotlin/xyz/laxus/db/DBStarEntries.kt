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
@TableName("STAR_ENTRIES")
@Columns(
    Column("STARRED_ID", BIGINT, unique = true),
    Column("ENTRY_ID", BIGINT, nullable = true, def = "NULL"),
    Column("STARBOARD_ID", BIGINT),
    Column("GUILD_ID", BIGINT, unique = true),
    Column("USER_ID", BIGINT, unique = true)
)
object DBStarEntries : Table() {
    fun addStar(starredId: Long, guildId: Long, userId: Long, entryId: Long? = null) {
        val starboardId = checkNotNull(DBStarSettings.getSettings(guildId)?.channelId) {
            "No StarSettings found for Guild ID: $guildId"
        }

        connection.prepare("SELECT * FROM STAR_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ? AND USER_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement[3] = userId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["STARRED_ID"] = starredId
                    it["ENTRY_ID"] = entryId
                    it["STARBOARD_ID"] = starboardId
                    it["GUILD_ID"] = guildId
                    it["USER_ID"] = userId
                }
            }
        }

        entryId?.let { setEntry(entryId, starredId, guildId) }
    }

    fun getStars(starredId: Long, guildId: Long): List<Long> {
        val stars = ArrayList<Long>()
        connection.prepare("SELECT USER_ID FROM STAR_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?") { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement.executeQuery {
                it.whileNext { stars += it.getLong("USER_ID") }
            }
        }
        return stars
    }

    fun isStarring(starredId: Long, guildId: Long, userId: Long): Boolean {
        return connection.prepare("SELECT * FROM STAR_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ? AND USER_ID = ?") { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement[3] = userId
            statement.executeQuery { it.next() }
        }
    }

    fun getStarCount(starredId: Long, guildId: Long): Int {
        var count = 0
        connection.prepare("SELECT * FROM STAR_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?") { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement.executeQuery {
                it.whileNext { count++ }
            }
        }
        return count
    }

    fun removeEntry(userId: Long, starredId: Long, guildId: Long) {
        connection.prepare("SELECT * FROM STAR_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ? AND USER_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = userId
            statement[2] = starredId
            statement[3] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeAllEntries(starredId: Long, guildId: Long) {
        connection.prepare("SELECT * FROM STAR_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }

    fun setEntry(entryId: Long, starredId: Long, guildId: Long) {
        connection.prepare("SELECT * FROM STAR_ENTRIES WHERE STARRED_ID = ? AND GUILD_ID = ? AND ENTRY_ID IS NOT NULL", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = starredId
            statement[2] = guildId
            statement.executeQuery {
                it.whileNext {
                    it.update { it["ENTRY_ID"] = entryId }
                }
            }
        }
    }
}