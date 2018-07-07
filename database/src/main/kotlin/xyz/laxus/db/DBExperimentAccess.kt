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

import xyz.laxus.db.entities.ExperimentAccess
import xyz.laxus.db.schema.*
import xyz.laxus.db.sql.*
import xyz.laxus.db.sql.ResultSetConcur.*
import xyz.laxus.db.sql.ResultSetType.*

@TableName("experiment_access")
@Columns(
    Column("id", BIGINT, primary = true),
    Column("access_type", "$VARCHAR(20)", primary = true),
    Column("access_level", "$VARCHAR(20)")
)
object DBExperimentAccess: Table() {
    private const val query = "SELECT * FROM experiment_access WHERE id = ? AND access_type = ?"

    fun getExperimentAccess(id: Long, type: ExperimentAccess.Type): ExperimentAccess? {
        connection.prepare(query) { statement ->
            statement[1] = id
            statement[2] = type.name
            statement.executeQuery {
                if(it.next()) {
                    return ExperimentAccess(
                        id = it.getLong("id"),
                        level = ExperimentAccess.Level.valueOf(it.getString("access_level")),
                        type = ExperimentAccess.Type.valueOf(it.getString("access_type"))
                    )
                }
            }
        }
        return null
    }

    fun setExperimentAccess(access: ExperimentAccess) {
        connection.prepare(query, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = access.id
            statement[2] = access.type.toString()
            statement.executeQuery {
                if(it.next()) it.update {
                    it["access_level"] = access.level.toString()
                } else it.insert {
                    it["id"] = access.id
                    it["access_type"] = access.type.toString()
                    it["access_level"] = access.level.toString()
                }
            }
        }
    }

    fun removeExperimentAccess(id: Long, type: ExperimentAccess.Type) {
        connection.prepare(query, SCROLL_INSENSITIVE, UPDATABLE) { statement ->
            statement[1] = id
            statement[2] = type.toString()
            statement.executeQuery { if(it.next()) it.deleteRow() }
        }
    }
}