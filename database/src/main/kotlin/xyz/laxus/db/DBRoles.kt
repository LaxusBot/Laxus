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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package xyz.laxus.db

import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*

/**
 * @author Kaidan Gustave
 */
@TableName("GUILD_ROLES") @Columns(
    Column("GUILD_ID", BIGINT, unique = true),
    Column("ROLE_ID", BIGINT, unique = true),
    Column("TYPE", "$VARCHAR(50)", unique = true)
)
object DBRoles : Table() {
    private const val GET_ROLES = "SELECT ROLE_ID FROM GUILD_ROLES WHERE GUILD_ID = ? AND TYPE = ?"
    private const val SET_ADD_ROLES = "SELECT * FROM GUILD_ROLES WHERE GUILD_ID = ? AND TYPE = ?"
    private const val REMOVE_ROLE = "SELECT * FROM GUILD_ROLES WHERE GUILD_ID = ? AND ROLE_ID = ? AND TYPE = ?"

    fun isRole(guildId: Long, type: Type): Boolean {
        return connection.prepare(SET_ADD_ROLES) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery { it.next() }
        }
    }

    fun getRole(guildId: Long, type: Type): Long? {
        connection.prepare(GET_ROLES) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(it.next()) {
                    return it.getLong("ROLE_ID")
                }
            }
        }
        return null
    }

    fun getRoles(guildId: Long, type: Type): List<Long> {
        val roles = ArrayList<Long>()
        connection.prepare(GET_ROLES) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                it.whileNext { roles += it.getLong("ROLE_ID") }
            }
        }
        return roles
    }

    fun setRole(guildId: Long, roleId: Long, type: Type) {
        connection.prepare(SET_ADD_ROLES, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["GUILD_ID"] = guildId
                    it["ROLE_ID"] = roleId
                    it["TYPE"] = type.name
                } else it.update {
                    it["GUILD_ID"] = guildId
                    it["ROLE_ID"] = roleId
                    it["TYPE"] = type.name
                }
            }
        }
    }

    fun addRole(guildId: Long, roleId: Long, type: Type) {
        connection.prepare(REMOVE_ROLE, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = roleId
            statement[3] = type.name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["GUILD_ID"] = guildId
                    it["ROLE_ID"] = roleId
                    it["TYPE"] = type.name
                }
            }
        }
    }

    fun removeRole(guildId: Long, roleId: Long, type: Type) {
        connection.prepare(REMOVE_ROLE, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = roleId
            statement[3] = type.name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeRole(guildId: Long, type: Type) {
        removeRoles(guildId, type)
    }

    fun removeRoles(guildId: Long, type: Type) {
        connection.prepare(GET_ROLES, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }

    fun removeAllRoles(guildId: Long) {
        for(type in Type.values()) {
            removeRoles(guildId, type)
        }
    }

    enum class Type {
        ANNOUNCEMENTS,
        ROLE_ME,
        COLOR_ME,
        MUTED,
        IGNORED,
        MODERATOR
    }
}