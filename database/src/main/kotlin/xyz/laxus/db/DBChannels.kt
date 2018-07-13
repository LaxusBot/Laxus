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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package xyz.laxus.db

import xyz.laxus.db.annotation.AllPrimary
import xyz.laxus.db.annotation.Column
import xyz.laxus.db.annotation.Columns
import xyz.laxus.db.annotation.TableName
import xyz.laxus.db.sql.*

/**
 * @author Kaidan Gustave
 */
@AllPrimary
@TableName("guild_channels")
@Columns(
    Column("guild_id", "BIGINT"),
    Column("channel_id", "BIGINT"),
    Column("type", "VARCHAR(50)")
)
object DBChannels : Table() {
    fun isChannel(guildId: Long, channelId: Long, type: Type): Boolean {
        return connection.prepare("SELECT * FROM guild_channels WHERE guild_id = ? AND channel_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = channelId
            statement[3] = type.name
            statement.executeQuery { it.next() }
        }
    }

    fun hasChannel(guildId: Long, type: Type): Boolean {
        require(type.single, type::notSingle)
        return connection.prepare("SELECT channel_id FROM guild_channels WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery { it.next() }
        }
    }

    fun getChannel(guildId: Long, type: Type): Long? {
        require(type.single, type::notSingle)
        connection.prepare("SELECT channel_id FROM guild_channels WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(it.next()) {
                    return it.getLong("channel_id")
                }
            }
        }
        return null
    }

    fun getChannels(guildId: Long, type: Type): List<Long> {
        require(type.multi, type::notMulti)
        val channels = ArrayList<Long>()
        connection.prepare("SELECT channel_id FROM guild_channels WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                while(it.next()) {
                    channels += it.getLong("channel_id")
                }
            }
        }
        return channels
    }

    fun setChannel(guildId: Long, channelId: Long, type: Type) {
        require(type.single, type::notSingle)
        connection.update("SELECT * FROM guild_channels WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["channel_id"] = channelId
                    it["type"] = type.name
                } else it.update {
                    it["guild_id"] = guildId
                    it["channel_id"] = channelId
                    it["type"] = type.name
                }
            }
        }
    }

    fun addChannel(guildId: Long, channelId: Long, type: Type) {
        require(type.multi, type::notMulti)
        connection.update("SELECT * FROM guild_channels WHERE guild_id = ? AND channel_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = channelId
            statement[3] = type.name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["channel_id"] = channelId
                    it["type"] = type.name
                }
            }
        }
    }

    fun removeChannel(guildId: Long, type: Type) {
        require(type.single, type::notSingle)
        connection.update("SELECT * FROM guild_channels WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeChannel(guildId: Long, channelId: Long, type: Type) {
        require(type.multi, type::notMulti)
        connection.update("SELECT * FROM guild_channels WHERE guild_id = ? AND channel_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = channelId
            statement[3] = type.name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeChannels(guildId: Long, type: Type) {
        require(type.multi, type::notMulti)
        connection.update("SELECT * FROM guild_channels WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                while(it.next()) it.deleteRow()
            }
        }
    }

    fun removeAllChannels(guildId: Long) {
        for(type in Type.values()) {
            if(type.single) {
                removeChannel(guildId, type)
            } else {
                removeChannels(guildId, type)
            }
        }
    }

    enum class Type(val single: Boolean) {
        ANNOUNCEMENTS(true),
        IGNORED(false),
        MOD_LOG(true),
        WELCOME(true);

        val multi get() = !single

        internal fun notMulti() = "Channel type '$this' is not a multi-type!"
        internal fun notSingle() = "Channel type '$this' is not a single-type!"
    }
}