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

import xyz.laxus.db.annotation.Column
import xyz.laxus.db.annotation.Columns
import xyz.laxus.db.annotation.TableName
import xyz.laxus.db.sql.*

/**
 * @author Kaidan Gustave
 */
@TableName("afk_users")
@Columns(
    Column("user_id", "BIGINT", primary = true),
    Column("afk_message", "VARCHAR(500)", def = "")
)
object DBAFKUsers: Table() {
    fun getAFKMessage(userId: Long): String? {
        connection.prepare("SELECT afk_message FROM afk_users WHERE user_id = ?") { statement ->
            statement[1] = userId
            statement.executeQuery {
                if(it.next()) {
                    return it.getString("afk_message")
                }
            }
        }
        return null
    }

    fun setAFK(userId: Long, afkMessage: String = "") {
        connection.update("SELECT * FROM afk_users WHERE user_id = ?") { statement ->
            statement[1] = userId
            statement.executeQuery {
                if(it.next()) it.update {
                    it["afk_message"] = afkMessage
                } else it.insert {
                    it["user_id"] = userId
                    it["afk_message"] = afkMessage
                }
            }
        }
    }

    fun removeAFK(userId: Long) {
        connection.update("SELECT * FROM afk_users WHERE user_id = ?") { statement ->
            statement[1] = userId
            statement.executeQuery {
                if(it.next()) {
                    it.deleteRow()
                }
            }
        }
    }
}