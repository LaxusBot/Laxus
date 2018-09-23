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

import xyz.laxus.db.annotation.AllPrimary
import xyz.laxus.db.annotation.Column
import xyz.laxus.db.annotation.Columns
import xyz.laxus.db.annotation.TableName
import xyz.laxus.db.sql.*

@AllPrimary
@TableName("prefixes")
@Columns(
    Column("guild_id", "BIGINT"),
    Column("prefix", "VARCHAR(50)")
)
object DBPrefixes: Table() {
    fun hasPrefix(guildId: Long, prefix: String): Boolean {
        return connection.prepare("SELECT * FROM prefixes WHERE guild_id = ? AND LOWER(prefix) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = prefix
            statement.executeQuery { it.next() }
        }
    }

    fun getPrefixes(guildId: Long): Set<String> {
        val prefixes = HashSet<String>()
        connection.prepare("SELECT prefix FROM prefixes WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                while(it.next()) {
                    prefixes += it.getString("prefix")
                }
            }
        }
        return prefixes
    }

    fun addPrefix(guildId: Long, prefix: String) {
        connection.update("SELECT * FROM prefixes WHERE guild_id = ? AND LOWER(prefix) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = prefix
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["prefix"] = prefix
                }
            }
        }
    }

    fun removePrefix(guildId: Long, prefix: String) {
        connection.update("SELECT * FROM prefixes WHERE guild_id = ? AND LOWER(prefix) = LOWER(?)") { statement ->
            statement[1] = guildId
            statement[2] = prefix
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeAllPrefixes(guildId: Long) {
        connection.update("SELECT * FROM prefixes WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                while(it.next()) it.deleteRow()
            }
        }
    }
}
