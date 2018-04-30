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

@TableName("command_levels")
@Columns(
    Column("guild_id", BIGINT, primary = true),
    Column("command_name", "$VARCHAR(100)", primary = true),
    Column("command_level", "$VARCHAR(25)")
)
object DBCommandLevels: Table() {
    fun getCommandLevel(guildId: Long, commandName: String): String? {
        connection.prepare("SELECT command_level FROM command_levels WHERE guild_id = ? AND LOWER(command_name) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = commandName
            statement.executeQuery {
                if(it.next()) {
                    return it.getString("command_level").toUpperCase()
                }
            }
        }
        return null
    }

    fun setCommandLevel(guildId: Long, commandName: String, commandLevel: String?) {
        connection.prepare("SELECT * FROM command_levels WHERE guild_id = ? AND LOWER(command_name) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = commandName
            statement.executeQuery {
                if(it.next()) { // data exists
                    if(commandLevel === null) it.deleteRow() // delete
                    else it.update { // update
                        it["command_level"] = commandLevel.toUpperCase()
                    }
                } else if(commandLevel !== null) it.insert { // insert
                    it["guild_id"] = guildId
                    it["command_name"] = commandName.toUpperCase()
                    it["command_level"] = commandLevel.toUpperCase()
                }
                // else do nothing
            }
        }
    }
}