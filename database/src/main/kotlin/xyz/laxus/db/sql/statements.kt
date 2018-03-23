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

import org.h2.value.Value
import org.intellij.lang.annotations.Language
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

inline fun <reified R> Connection.prepare(
    @Language("sql") sql: String,
    type: ResultSetType = ResultSetType.FORWARD_ONLY,
    concur: ResultSetConcur = ResultSetConcur.READ_ONLY,
    block: (PreparedStatement) -> R
): R = prepareStatement(sql, type.resultSetInt, concur.resultSetInt).use(block)

inline fun <reified R> PreparedStatement.executeQuery(block: (ResultSet) -> R): R {
    return executeQuery().use(block)
}

inline operator fun <reified S: PreparedStatement> S.set(index: Int, value: String?) {
    if(value === null) {
        setNull(index, Value.STRING)
    } else {
        setString(index, value)
    }
}

inline operator fun <reified S: PreparedStatement> S.set(index: Int, value: Short?) {
    if(value === null) {
        setNull(index, Value.SHORT)
    } else {
        setShort(index, value)
    }
}

inline operator fun <reified S: PreparedStatement> S.set(index: Int, value: Int?) {
    if(value === null) {
        setNull(index, Value.INT)
    } else {
        setInt(index, value)
    }
}

inline operator fun <reified S: PreparedStatement> S.set(index: Int, value: Long?) {
    if(value === null) {
        setNull(index, Value.LONG)
    } else {
        setLong(index, value)
    }
}

inline operator fun <reified S: PreparedStatement> S.set(index: Int, value: Float?) {
    if(value === null) {
        setNull(index, Value.FLOAT)
    } else {
        setFloat(index, value)
    }
}

inline operator fun <reified S: PreparedStatement> S.set(index: Int, value: Double?) {
    if(value === null) {
        setNull(index, Value.DOUBLE)
    } else {
        setDouble(index, value)
    }
}
