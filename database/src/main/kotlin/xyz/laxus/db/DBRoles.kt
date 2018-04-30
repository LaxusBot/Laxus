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
@AllPrimary
@TableName("guild_roles")
@Columns(
    Column("guild_id", BIGINT),
    Column("role_id", BIGINT),
    Column("type", "$VARCHAR(50)")
)
object DBRoles: Table() {
    fun isRole(guildId: Long, roleId: Long, type: Type): Boolean {
        return connection.prepare("SELECT * FROM guild_roles WHERE guild_id = ? AND role_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = roleId
            statement[3] = type.name
            statement.executeQuery { it.next() }
        }
    }

    fun hasRole(guildId: Long, type: Type): Boolean {
        require(type.single, type::notSingle)
        return connection.prepare("SELECT * FROM guild_roles WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery { it.next() }
        }
    }

    fun getRole(guildId: Long, type: Type): Long? {
        require(type.single, type::notSingle)
        connection.prepare("SELECT role_id FROM guild_roles WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(it.next()) {
                    return it.getLong("role_id")
                }
            }
        }
        return null
    }

    fun getRoles(guildId: Long, type: Type): List<Long> {
        require(type.multi, type::notMulti)
        val roles = ArrayList<Long>()
        connection.prepare("SELECT role_id FROM guild_roles WHERE guild_id = ? AND type = ?") { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                it.whileNext { roles += it.getLong("role_id") }
            }
        }
        return roles
    }

    fun setRole(guildId: Long, roleId: Long, type: Type) {
        require(type.single, type::notSingle)
        connection.prepare("SELECT * FROM guild_roles WHERE guild_id = ? AND type = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["role_id"] = roleId
                    it["type"] = type.name
                } else it.update {
                    it["role_id"] = roleId
                }
            }
        }
    }

    fun addRole(guildId: Long, roleId: Long, type: Type) {
        require(type.multi, type::notMulti)
        connection.prepare("SELECT * FROM guild_roles WHERE guild_id = ? AND role_id = ? AND type = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = roleId
            statement[3] = type.name
            statement.executeQuery {
                if(!it.next()) it.insert {
                    it["guild_id"] = guildId
                    it["role_id"] = roleId
                    it["type"] = type.name
                }
            }
        }
    }

    fun removeRole(guildId: Long, type: Type) {
        require(type.single, type::notSingle)
        connection.prepare("SELECT role_id FROM guild_roles WHERE guild_id = ? AND type = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeRole(guildId: Long, roleId: Long, type: Type) {
        require(type.multi, type::notMulti)
        connection.prepare("SELECT * FROM guild_roles WHERE guild_id = ? AND role_id = ? AND type = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = roleId
            statement[3] = type.name
            statement.executeQuery {
                if(it.next()) it.deleteRow()
            }
        }
    }

    fun removeRoles(guildId: Long, type: Type) {
        require(type.multi, type::notMulti)
        connection.prepare("SELECT * FROM guild_roles WHERE guild_id = ? AND type = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement[2] = type.name
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }

    fun removeAllRoles(guildId: Long) {
        for(type in Type.values()) {
            if(type.single) {
                removeRole(guildId, type)
            } else {
                removeRoles(guildId, type)
            }
        }
    }

    enum class Type(val single: Boolean) {
        ANNOUNCEMENTS(false),
        ROLE_ME(false),
        COLOR_ME(false),
        MUTED(true),
        IGNORED(false),
        MODERATOR(true);

        inline val multi get() = !single

        internal fun notMulti() = "Role type '$this' is not a multi-type!"
        internal fun notSingle() = "Role type '$this' is not a single-type!"
    }
}