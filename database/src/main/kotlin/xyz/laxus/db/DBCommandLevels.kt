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

@TableName("COMMAND_LEVELS")
@Columns(
    Column("GUILD_ID", BIGINT, unique = true),
    Column("COMMAND_NAME", "$VARCHAR(100)", unique = true),
    Column("COMMAND_LEVEL", "$VARCHAR(25)")
)
object DBCommandLevels : Table() {
    fun getCommandLevel(guildId: Long, commandName: String): String? {
        connection.prepare("SELECT COMMAND_LEVEL FROM COMMAND_LEVELS WHERE GUILD_ID = ? AND LOWER(COMMAND_NAME) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = commandName
            statement.executeQuery {
                if(it.next()) {
                    return it.getString("COMMAND_LEVEL").toUpperCase()
                }
            }
        }
        return null
    }

    fun setCommandLevel(guildId: Long, commandName: String, commandLevel: String?) {
        connection.prepare("SELECT * FROM COMMAND_LEVELS WHERE GUILD_ID = ? AND LOWER(COMMAND_NAME) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = commandName
            statement.executeQuery {
                if(it.next()) { // data exists
                    if(commandLevel === null) it.deleteRow() // delete
                    else it.update { // update
                        it["COMMAND_LEVEL"] = commandLevel.toUpperCase()
                    }
                } else if(commandLevel !== null) it.insert { // insert
                    it["GUILD_ID"] = guildId
                    it["COMMAND_NAME"] = commandName.toUpperCase()
                    it["COMMAND_LEVEL"] = commandLevel.toUpperCase()
                }
                // else do nothing
            }
        }
    }
}