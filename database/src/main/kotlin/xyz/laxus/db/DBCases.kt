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
@TableName("CASES")
@Columns(
    Column("CASE_NUMBER", INT, unique = true),
    Column("GUILD_ID", BIGINT, unique = true),
    Column("MOD_ID", BIGINT),
    Column("TARGET_ID", BIGINT),
    Column("IS_ON_USER", BOOLEAN),
    Column("ACTION", "$VARCHAR(50)"),
    Column("REASON", "$VARCHAR(300)", nullable = true, default = "NULL")
)
object DBCases : Table() {
    private const val GET_CASES           = "SELECT * FROM CASES WHERE GUILD_ID = ? ORDER BY CASE_NUMBER DESC"
    private const val GET_CASE_BY_NUMBER  = "SELECT * FROM CASES WHERE CASE_NUMBER = ? AND GUILD_ID = ?"
    private const val GET_CASES_BY_MOD_ID = "SELECT * FROM CASES WHERE GUILD_ID = ? AND MOD_ID = ? ORDER BY CASE_NUMBER DESC"
    private const val SET_REASON          = "SELECT REASON FROM CASES WHERE CASE_NUMBER = ? AND GUILD_ID = ?"

    fun getCurrentCaseNumber(guildId: Long): Int {
        connection.prepare(GET_CASES) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return it.getInt("CASE_NUMBER")
                }
            }
        }
        return 0
    }

    fun getCases(guildId: Long): List<Case> {
        val cases = ArrayList<Case>()
        connection.prepare(GET_CASES) { statement ->
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
        connection.prepare(GET_CASE_BY_NUMBER) { statement ->
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
        connection.prepare(GET_CASES_BY_MOD_ID) { statement ->
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
        connection.prepare(GET_CASES, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = case.guildId
            statement.executeQuery {
                it.insert {
                    it["CASE_NUMBER"] = case.number
                    it["GUILD_ID"] = case.guildId
                    it["MOD_ID"] = case.modId
                    it["TARGET_ID"] = case.targetId
                    it["IS_ON_USER"] = case.isOnUser
                    it["ACTION"] = case.action.name
                    it["REASON"] = case.reason
                }
            }
        }
    }

    fun updateCase(case: Case) {
        connection.prepare(SET_REASON, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = case.number
            statement[2] = case.guildId
            statement.executeQuery {
                if(it.next()) {
                    it["REASON"] = case.reason
                }
            }
        }
    }

    fun removeAllCases(guildId: Long) {
        connection.prepare(GET_CASES, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = guildId
            statement.executeQuery {
                it.whileNext { it.deleteRow() }
            }
        }
    }
}