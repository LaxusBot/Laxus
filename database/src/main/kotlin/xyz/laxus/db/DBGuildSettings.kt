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
@TableName("GUILD_SETTINGS")
@Columns(
    Column("GUILD_ID", BIGINT, unique = true),
    Column("IS_ROLE_PERSIST", BOOLEAN, def = "false"),
    Column("ROLE_ME_LIMIT", SMALLINT, nullable = true, def = "null")
)
object DBGuildSettings : Table() {
    private const val GUILD_ID = "GUILD_ID"
    private const val IS_ROLE_PERSIST = "IS_ROLE_PERSIST"
    private const val ROLE_ME_LIMIT = "ROLE_ME_LIMIT"

    fun isRolePersist(guildId: Long): Boolean {
        connection.prepare("SELECT $IS_ROLE_PERSIST FROM GUILD_SETTINGS WHERE $GUILD_ID = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return it.getBoolean(IS_ROLE_PERSIST)
                }
            }
        }
        return false
    }

    fun getRoleMeLimit(guildId: Long): Short? {
        connection.prepare("SELECT $ROLE_ME_LIMIT FROM GUILD_SETTINGS WHERE $GUILD_ID = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return it.getNullShort(ROLE_ME_LIMIT)
                }
            }
        }
        return null
    }

    fun setIsRolePersist(guildId: Long, isRolePersist: Boolean) {
        connection.prepare("SELECT * FROM GUILD_SETTINGS WHERE $GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it[GUILD_ID] = guildId
                    it[IS_ROLE_PERSIST] = isRolePersist
                } else it.update {
                    it[IS_ROLE_PERSIST] = isRolePersist
                }
            }
        }
        if(!isRolePersist) {
            DBRolePersist.removeAllRolePersist(guildId)
        }
    }

    fun setRoleMeLimit(guildId: Long, roleMeLimit: Short?) {
        connection.prepare("SELECT * FROM GUILD_SETTINGS WHERE $GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it[GUILD_ID] = guildId
                    it[ROLE_ME_LIMIT] = roleMeLimit
                } else it.update {
                    it[ROLE_ME_LIMIT] = roleMeLimit
                }
            }
        }
    }

    fun removeSettings(guildId: Long) {
        connection.prepare("SELECT * FROM GUILD_SETTINGS WHERE $GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
        DBRolePersist.removeAllRolePersist(guildId)
    }
}