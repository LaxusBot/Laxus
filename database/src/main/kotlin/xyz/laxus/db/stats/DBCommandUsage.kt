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
package xyz.laxus.db.stats

import kotlinx.coroutines.experimental.*
import xyz.laxus.db.Table
import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*

/**
 * @author Kaidan Gustave
 */
@TableName("command_usage")
@Columns(
    Column("use_id", "BIGSERIAL", primary = true),
    Column("command_name", "$VARCHAR(50)", primary = true),
    Column("guild_id", BIGINT, nullable = true, def = "NULL"),
    Column("channel_id", BIGINT),
    Column("user_id", BIGINT)
)
object DBCommandUsage: Table() {
    private val usageContext by lazy { newSingleThreadContext("CommandUsage-Thread Context") }

    fun addUsage(commandName: String, guildId: Long?, channelId: Long, userId: Long) {
        launch(usageContext) {
            connection.prepare("SELECT * FROM command_usage WHERE LOWER(command_name) = LOWER(?)", SCROLL_INSENSITIVE, UPDATABLE) { statement ->
                statement[1] = commandName
                statement.executeQuery {
                    if(!it.next()) it.insert {
                        it["command_name"] = commandName
                        it["guild_id"] = guildId
                        it["channel_id"] = channelId
                        it["user_id"] = userId
                    }
                }
            }
        }
    }

    suspend fun getUsage(commandName: String, guildId: Long? = null): Long = async(usageContext) {
        if(guildId === null) {
            connection.prepare("SELECT COUNT(*) FROM command_usage WHERE LOWER(command_name) = LOWER(?)") { statement ->
                statement[1] = commandName
                statement.executeQuery {
                    if(it.next()) {
                        return@async it.getLong("COUNT(*)")
                    }
                }
            }
            return@async 0L
        }
        connection.prepare("SELECT COUNT(*) FROM command_usage WHERE LOWER(command_name) = LOWER(?) AND guild_id = ?") { statement ->
            statement[1] = commandName
            statement[2] = guildId
            statement.executeQuery {
                if(it.next()) {
                    return@async it.getLong("COUNT(*)")
                }
            }
        }
        return@async 0L
    }.await()

    override fun close() {
        usageContext.cancel(CancellationException("CommandUsage-Thread Context was closed"))
        usageContext.close()
    }
}