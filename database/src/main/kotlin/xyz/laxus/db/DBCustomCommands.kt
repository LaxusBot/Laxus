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

@TableName("custom_commands")
@Columns(
    Column("guild_id", BIGINT, primary = true),
    Column("name", "$VARCHAR(50)", primary = true),
    Column("content", "$VARCHAR(1900)")
)
object DBCustomCommands: Table() {
    fun hasCustomCommand(guildId: Long, name: String): Boolean {
        return connection.prepare("SELECT * FROM custom_commands WHERE guild_id = ? AND LOWER(name) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = name
            statement.executeQuery { it.next() }
        }
    }

    fun getCustomCommands(guildId: Long): List<Pair<String, String>> {
        val list = ArrayList<Pair<String, String>>()
        connection.prepare("SELECT * FROM custom_commands WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext {
                    list += (it.getString("name") to it.getString("content"))
                }
            }
        }
        return list
    }

    fun getCustomCommand(guildId: Long, name: String): String? {
        connection.prepare("SELECT * FROM custom_commands WHERE guild_id = ? AND LOWER(name) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = name
            statement.executeQuery {
                if(it.next()) return it.getString("content")
            }
        }
        return null
    }

    fun setCustomCommand(guildId: Long, name: String, content: String) {
        require(name.length <= 50) { "Custom Command name length exceeds maximum of 50 characters!" }
        require(content.length <= 1900) { "Custom Command content length exceeds maximum of 50 characters!" }
        connection.prepare("SELECT * FROM custom_commands WHERE guild_id = ? AND LOWER(name) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["name"] = name
                    it["content"] = content
                } else it.update {
                    it["content"] = content
                }
            }
        }
    }

    fun removeCustomCommand(guildId: Long, name: String) {
        connection.prepare("SELECT * FROM custom_commands WHERE guild_id = ? AND LOWER(name) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }
}