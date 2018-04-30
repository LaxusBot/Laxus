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
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*
import xyz.laxus.db.sql.executeQuery
import xyz.laxus.db.sql.insert
import xyz.laxus.db.sql.prepare
import xyz.laxus.db.sql.set

/**
 * @author Kaidan Gustave
 */
@AllPrimary
@TableName("guilds")
@Columns(
    Column("guild_id", BIGINT),
    Column("type", "$VARCHAR(50)")
)
object DBGuilds: Table() {
    fun isGuild(guildId: Long, type: Type): Boolean {
        return connection.prepare("SELECT * FROM guilds WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                it.next()
            }
        }
    }

    fun addGuild(guildId: Long, type: Type) {
        connection.prepare("SELECT * FROM guilds WHERE guild_id = ? AND type = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["type"] = type.name
                }
            }
        }
    }

    fun removeGuild(guildId: Long, type: Type) {
        connection.prepare("SELECT * FROM guilds WHERE guild_id = ? AND type = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(it.next()) {
                    it.deleteRow()
                }
            }
        }
    }

    enum class Type {
        MUSIC,
        BLACKLIST,
        WHITELIST
    }
}