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
import xyz.laxus.db.sql.*

/**
 * @author Kaidan Gustave
 */
@TableName("welcomes")
@Columns(
    Column("guild_id", "BIGINT", primary = true),
    Column("message", "VARCHAR(1900)")
)
object DBWelcomes: Table() {
    fun hasWelcome(guildId: Long): Boolean {
        return DBChannels.hasChannel(guildId, DBChannels.Type.WELCOME)
    }

    fun getWelcome(guildId: Long): Pair<Long, String>? {
        val channelId = DBChannels.getChannel(guildId, DBChannels.Type.WELCOME) ?: return null
        connection.prepare("SELECT message FROM welcomes WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return channelId to it.getString("message")
                }
            }
        }
        return null
    }

    fun setWelcome(guildId: Long, channelId: Long, message: String) {
        require(message.length <= 1900) { "Welcome message must not be longer than 1900 characters!" }
        DBChannels.setChannel(guildId, channelId, DBChannels.Type.WELCOME)
        connection.update("SELECT * FROM welcomes WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["message"] = message
                } else it.update {
                    it["message"] = message
                }
            }
        }
    }

    fun removeWelcome(guildId: Long) {
        DBChannels.removeChannel(guildId, DBChannels.Type.WELCOME)
        connection.update("SELECT * FROM welcomes WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }
}