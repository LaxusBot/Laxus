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

@TableName("CUSTOM_COMMANDS")
@Columns(
    Column("GUILD_ID", BIGINT, unique = true),
    Column("NAME", "$VARCHAR(50)", unique = true),
    Column("CONTENT", "$VARCHAR(1900)")
)
object DBCustomCommands : Table() {
    fun hasCustomCommand(guildId: Long, name: String): Boolean {
        return connection.prepare("SELECT * FROM CUSTOM_COMMANDS WHERE GUILD_ID = ? AND LOWER(NAME) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = name
            statement.executeQuery { it.next() }
        }
    }

    fun getCustomCommands(guildId: Long): List<Pair<String, String>> {
        val list = ArrayList<Pair<String, String>>()
        connection.prepare("SELECT * FROM CUSTOM_COMMANDS WHERE GUILD_ID = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext {
                    list += (it.getString("NAME") to it.getString("CONTENT"))
                }
            }
        }
        return list
    }

    fun getCustomCommand(guildId: Long, name: String): String? {
        connection.prepare("SELECT * FROM CUSTOM_COMMANDS WHERE GUILD_ID = ? AND LOWER(NAME) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = name
            statement.executeQuery {
                if(it.next()) return it.getString("CONTENT")
            }
        }
        return null
    }

    fun setCustomCommand(guildId: Long, name: String, content: String) {
        require(name.length <= 50) { "Custom Command name length exceeds maximum of 50 characters!" }
        require(content.length <= 1900) { "Custom Command content length exceeds maximum of 50 characters!" }
        connection.prepare("SELECT * FROM CUSTOM_COMMANDS WHERE GUILD_ID = ? AND LOWER(NAME) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["NAME"] = name
                    it["CONTENT"] = content
                } else it.update {
                    it["CONTENT"] = content
                }
            }
        }
    }

    fun removeCustomCommand(guildId: Long, name: String) {
        connection.prepare("SELECT * FROM CUSTOM_COMMANDS WHERE GUILD_ID = ? AND LOWER(NAME) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }
}