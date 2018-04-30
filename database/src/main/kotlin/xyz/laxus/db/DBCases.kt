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

import xyz.laxus.db.entities.Case
import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*

/**
 * @author Kaidan Gustave
 */
@TableName("cases")
@Columns(
    Column("case_number", INT, primary = true),
    Column("guild_id", BIGINT, primary = true),
    Column("message_id", BIGINT),
    Column("mod_id", BIGINT),
    Column("target_id", BIGINT),
    Column("is_on_user", BOOLEAN),
    Column("action", "$VARCHAR(50)"),
    Column("reason", "$VARCHAR(300)", nullable = true, def = "NULL")
)
object DBCases: Table() {
    fun getCurrentCaseNumber(guildId: Long): Int {
        connection.prepare("SELECT COUNT(*) FROM cases WHERE guild_id = ?") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return it.getInt("COUNT(*)")
                }
            }
        }
        return 0
    }

    fun getCases(guildId: Long): List<Case> {
        val cases = ArrayList<Case>()
        connection.prepare("SELECT * FROM cases WHERE guild_id = ? ORDER BY case_number ASC") { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext {
                    cases += Case(it)
                }
            }
        }
        return cases
    }

    fun getCase(number: Int, guildId: Long): Case? {
        connection.prepare("SELECT * FROM cases WHERE case_number = ? AND guild_id = ?") { statement ->
            statement[1] = number
            statement[2] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return Case(it)
                }
            }
        }
        return null
    }

    fun getCasesByModId(guildId: Long, modId: Long): List<Case> {
        val cases = ArrayList<Case>()
        connection.prepare("SELECT * FROM cases WHERE guild_id = ? AND mod_id = ? ORDER BY case_number ASC") { statement ->
            statement[1] = guildId
            statement[2] = modId
            statement.executeQuery {
                it.whileNext {
                    cases += Case(it)
                }
            }
        }
        return cases
    }

    fun getCasesWithoutReasonByModId(guildId: Long, modId: Long): List<Case> {
        val cases = ArrayList<Case>()
        connection.prepare("SELECT * FROM cases WHERE guild_id = ? AND mod_id = ? AND reason IS NULL ORDER BY case_number DESC") { statement ->
            statement[1] = guildId
            statement[2] = modId
            statement.executeQuery {
                it.whileNext {
                    cases += Case(it)
                }
            }
        }
        return cases
    }

    fun addCase(case: Case) {
        connection.prepare("SELECT * FROM cases WHERE guild_id = ? ORDER BY case_number ASC", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = case.guildId
            statement.executeQuery {
                it.insert {
                    it["case_number"] = case.number
                    it["guild_id"] = case.guildId
                    it["mod_id"] = case.modId
                    it["message_id"] = case.messageId
                    it["target_id"] = case.targetId
                    it["is_on_user"] = case.isOnUser
                    it["action"] = case.action.name
                    it["reason"] = case.reason
                }
            }
        }
    }

    fun updateCase(case: Case) {
        connection.prepare("SELECT reason FROM cases WHERE case_number = ? AND guild_id = ?", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = case.number
            statement[2] = case.guildId
            statement.executeQuery {
                if(it.next()) it.update {
                    it["reason"] = case.reason
                }
            }
        }
    }

    fun removeAllCases(guildId: Long) {
        connection.prepare("SELECT * FROM cases WHERE guild_id = ? ORDER BY case_number ASC", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }
}