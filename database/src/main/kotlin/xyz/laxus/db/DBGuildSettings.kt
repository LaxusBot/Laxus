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
@TableName("guild_settings")
@Columns(
    Column("guild_id", BIGINT, primary = true),
    Column("is_role_persist", BOOLEAN, def = "false"),
    Column("role_me_limit", SMALLINT, nullable = true, def = "null")
)
object DBGuildSettings: Table() {
    fun isRolePersist(guildId: Long): Boolean {
        connection.prepare("SELECT is_role_persist FROM guild_settings WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return it.getBoolean("is_role_persist")
                }
            }
        }
        return false
    }

    fun getRoleMeLimit(guildId: Long): Short? {
        connection.prepare("SELECT role_me_limit FROM guild_settings WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return it.getNullShort("role_me_limit")
                }
            }
        }
        return null
    }

    fun setIsRolePersist(guildId: Long, isRolePersist: Boolean) {
        connection.prepare("SELECT * FROM guild_settings WHERE guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["is_role_persist"] = isRolePersist
                } else it.update {
                    it["is_role_persist"] = isRolePersist
                }
            }
        }
        if(!isRolePersist) {
            DBRolePersist.removeAllRolePersist(guildId)
        }
    }

    fun setRoleMeLimit(guildId: Long, roleMeLimit: Short?) {
        connection.prepare("SELECT * FROM guild_settings WHERE guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["role_me_limit"] = roleMeLimit
                } else it.update {
                    it["role_me_limit"] = roleMeLimit
                }
            }
        }
    }

    fun removeSettings(guildId: Long) {
        connection.prepare("SELECT * FROM guild_settings WHERE guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
        DBRolePersist.removeAllRolePersist(guildId)
    }
}