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
@TableName("role_persist")
@Columns(
    Column("guild_id", BIGINT, primary = true),
    Column("user_id", BIGINT, primary = true),
    Column("role_ids", "$BIGINT[]")
)
object DBRolePersist: Table() {
    fun getRolePersist(guildId: Long, userId: Long): List<Long> {
        connection.prepare("SELECT role_ids FROM role_persist WHERE guild_id = ? AND user_id = ?") { statement ->
            statement[1] = guildId
            statement[2] = userId
            statement.executeQuery {
                if(it.next()) {
                    return it.array<Long>("role_ids").toList()
                }
            }
        }
        return emptyList()
    }

    fun setRolePersist(guildId: Long, userId: Long, roleIds: List<Long>) {
        connection.prepare("SELECT * FROM role_persist WHERE guild_id = ? AND user_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = userId
            statement.executeQuery {
                val array = sqlArrayOf(SQLArrayType.INT_8, *roleIds.toTypedArray())
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["user_id"] = userId
                    it["role_ids"] = array
                } else it.update {
                    it["role_ids"] = array
                }
            }
        }
    }

    fun removeRolePersist(guildId: Long, userId: Long) {
        connection.prepare("SELECT * FROM role_persist WHERE guild_id = ? AND user_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = userId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }

    fun removeAllRolePersist(guildId: Long) {
        connection.prepare("SELECT * FROM role_persist WHERE guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }
}