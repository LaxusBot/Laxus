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
@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.laxus.db

import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*

/**
 * @author Kaidan Gustave
 */
@TableName("guild_store")
@Columns(
    Column("shard_id", SMALLINT, nullable = true, def = "NULL"),
    Column("guild_id", BIGINT, primary = true)
)
object DBGuildStore: Table() {
    fun getGuilds(): Set<Long> {
        val set = HashSet<Long>()
        connection.prepare("SELECT guild_id FROM guild_store", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement.executeQuery {
                it.whileNext { set += it.getLong("guild_id") }
            }
        }
        return set
    }

    fun addGuild(guildId: Long) = addGuild(null, guildId)
    fun addGuild(shardId: Short?, guildId: Long) {
        connection.prepare("SELECT * FROM guild_store WHERE guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    // moved shards?
                    if(it.getNullShort("shard_id") != shardId) it.update {
                        it["shard_id"] = shardId
                    } else return // data is consistent
                } else it.insert {
                    it["shard_id"] = shardId
                    it["guild_id"] = guildId
                }
            }
        }
    }

    fun isStored(guildId: Long): Boolean {
        return connection.prepare("SELECT * FROM guild_store WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.next()
            }
        }
    }

    fun isStored(shardId: Short, guildId: Long): Boolean {
        return connection.prepare("SELECT * FROM guild_store WHERE shard_id = ? AND guild_id = ?") { statement ->
            statement[1] = shardId
            statement[2] = guildId
            statement.executeQuery {
                it.next()
            }
        }
    }

    fun removeGuild(guildId: Long) {
        connection.prepare("SELECT * FROM guild_store WHERE guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun clearNonGuilds(guildIds: Set<Long>) {
        connection.prepare("SELECT * FROM guild_store", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement.executeQuery {
                it.whileNext {
                    if(it.getLong("guild_id") !in guildIds) it.deleteRow()
                }
            }
        }
    }
}