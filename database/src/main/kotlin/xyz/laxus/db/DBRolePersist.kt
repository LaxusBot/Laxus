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
import xyz.laxus.util.ignored

/**
 * @author Kaidan Gustave
 */
@TableName("ROLE_PERSIST")
@Columns(
    Column("GUILD_ID", BIGINT, unique = true),
    Column("USER_ID", BIGINT, unique = true),
    Column("ROLE_IDS", "$VARCHAR(2000)")
)
object DBRolePersist: Table() {
    fun getRolePersist(guildId: Long, userId: Long): List<Long> {
        val list = ArrayList<Long>()
        connection.prepare("SELECT ROLE_IDS FROM ROLE_PERSIST WHERE GUILD_ID = ? AND USER_ID = ?") { statement ->
            statement[1] = guildId
            statement[2] = userId
            statement.executeQuery {
                if(it.next()) {
                    it.getString("ROLE_IDS").split('|').mapNotNullTo(list) {
                        ignored(null) { it.toLong() }
                    }
                }
            }
        }
        return list
    }

    fun setRolePersist(guildId: Long, userId: Long, roleIds: List<Long>) {
        connection.prepare("SELECT * FROM ROLE_PERSIST WHERE GUILD_ID = ? AND USER_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = userId
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["GUILD_ID"] = guildId
                    it["USER_ID"] = userId
                    it["ROLE_IDS"] = roleIds.joinToString("|")
                } else it.update {
                    it["ROLE_IDS"] = roleIds.joinToString("|")
                }
            }
        }
    }

    fun removeRolePersist(guildId: Long, userId: Long) {
        connection.prepare("SELECT * FROM ROLE_PERSIST WHERE GUILD_ID = ? AND USER_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = userId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }

    fun removeAllRolePersist(guildId: Long) {
        connection.prepare("SELECT * FROM ROLE_PERSIST WHERE GUILD_ID = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }
}