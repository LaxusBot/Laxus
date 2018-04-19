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

/**
 * @author Kaidan Gustave
 */
@TableName("WELCOMES")
@Columns(
    Column("GUILD_ID", BIGINT, unique = true),
    Column("MESSAGE", "$VARCHAR(1900)")
)
object DBWelcomes: Table() {
    fun hasWelcome(guildId: Long): Boolean {
        return DBChannels.hasChannel(guildId, DBChannels.Type.WELCOME)
    }

    fun getWelcome(guildId: Long): Pair<Long, String>? {
        val channelId = DBChannels.getChannel(guildId, DBChannels.Type.WELCOME) ?: return null
        connection.prepare("SELECT MESSAGE FROM WELCOMES WHERE GUILD_ID = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return channelId to it.getString("MESSAGE")
                }
            }
        }
        return null
    }

    fun setWelcome(guildId: Long, channelId: Long, message: String) {
        require(message.length <= 1900) { "Welcome message must not be longer than 1900 characters!" }
        DBChannels.setChannel(guildId, channelId, DBChannels.Type.WELCOME)
        connection.prepare("SELECT * FROM WELCOMES WHERE GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["GUILD_ID"] = guildId
                    it["MESSAGE"] = message
                } else it.update {
                    it["MESSAGE"] = message
                }
            }
        }
    }

    fun removeWelcome(guildId: Long) {
        DBChannels.removeChannel(guildId, DBChannels.Type.WELCOME)
        connection.prepare("SELECT * FROM WELCOMES WHERE GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }
}