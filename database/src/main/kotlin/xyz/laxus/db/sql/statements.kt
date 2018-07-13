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
package xyz.laxus.db.sql

import org.intellij.lang.annotations.Language
import xyz.laxus.db.Database
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types.*

inline fun <R> Connection.update(
    @Language("sql") sql: String,
    type: ResultSetType = ResultSetType.SCROLL_INSENSITIVE,
    block: (PreparedStatement) -> R
): R = prepare(sql, type, ResultSetConcur.UPDATABLE, block)

inline fun <R> Connection.select(
    @Language("sql") sql: String,
    type: ResultSetType = ResultSetType.FORWARD_ONLY,
    block: (PreparedStatement) -> R
): R = prepare(sql, type, ResultSetConcur.READ_ONLY, block)

inline fun <R> Connection.prepare(
    @Language("sql") sql: String,
    type: ResultSetType = ResultSetType.FORWARD_ONLY,
    concur: ResultSetConcur = ResultSetConcur.READ_ONLY,
    block: (PreparedStatement) -> R
): R {
    Database.log.trace("Preparing statement: '$sql' (Type: $type, Concur: $concur)")
    return prepareStatement(sql, type.resultSetInt, concur.resultSetInt).use(block)
}

inline fun <reified R> PreparedStatement.executeQuery(block: (ResultSet) -> R): R {
    return executeQuery().use(block)
}

operator fun PreparedStatement.set(index: Int, value: String?) {
    if(value === null) {
        setNull(index, VARCHAR)
    } else {
        setString(index, value)
    }
}

operator fun PreparedStatement.set(index: Int, value: Short?) {
    if(value === null) {
        setNull(index, SMALLINT)
    } else {
        setShort(index, value)
    }
}

operator fun PreparedStatement.set(index: Int, value: Int?) {
    if(value === null) {
        setNull(index, INTEGER)
    } else {
        setInt(index, value)
    }
}

operator fun PreparedStatement.set(index: Int, value: Long?) {
    if(value === null) {
        setNull(index, BIGINT)
    } else {
        setLong(index, value)
    }
}

operator fun PreparedStatement.set(index: Int, value: Float?) {
    if(value === null) {
        setNull(index, FLOAT)
    } else {
        setFloat(index, value)
    }
}

operator fun PreparedStatement.set(index: Int, value: Double?) {
    if(value === null) {
        setNull(index, DOUBLE)
    } else {
        setDouble(index, value)
    }
}

operator fun <E: Enum<E>> PreparedStatement.set(index: Int, upperCase: Boolean = true, value: E?) {
    set(index, value?.name?.let { if(upperCase) it.toUpperCase() else it })
}
