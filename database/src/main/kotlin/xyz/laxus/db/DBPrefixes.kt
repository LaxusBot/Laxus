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

@TableName("PREFIXES")
@Columns(
    Column("GUILD_ID", BIGINT, unique = true),
    Column("PREFIX", "$VARCHAR(50)", unique = true)
)
object DBPrefixes: Table() {
    fun hasPrefix(guildId: Long, prefix: String): Boolean {
        return connection.prepare("SELECT * FROM PREFIXES WHERE GUILD_ID = ? AND LOWER(PREFIX) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = prefix
            statement.executeQuery {
                it.next()
            }
        }
    }

    fun getPrefixes(guildId: Long): Set<String> {
        val prefixes = HashSet<String>()
        connection.prepare("SELECT PREFIX FROM PREFIXES WHERE GUILD_ID = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext {
                    prefixes += it.getString("PREFIX")
                }
            }
        }
        return prefixes
    }

    fun addPrefix(guildId: Long, prefix: String) {
        connection.prepare("SELECT * FROM PREFIXES WHERE GUILD_ID = ? AND LOWER(PREFIX) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = prefix
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["GUILD_ID"] = guildId
                    it["PREFIX"] = prefix
                }
            }
        }
    }

    fun removePrefix(guildId: Long, prefix: String) {
        connection.prepare("SELECT * FROM PREFIXES WHERE GUILD_ID = ? AND LOWER(PREFIX) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = prefix
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeAllPrefixes(guildId: Long) {
        connection.prepare("SELECT PREFIX FROM PREFIXES WHERE GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }
}
