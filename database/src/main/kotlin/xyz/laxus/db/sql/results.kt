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
@file:Suppress("unused")
package xyz.laxus.db.sql

import xyz.laxus.db.schema.SQLArray
import xyz.laxus.db.schema.SQLTime
import xyz.laxus.db.schema.SQLTimestamp
import java.sql.ResultSet

inline fun <reified R: ResultSet> R.whileNext(block: (R) -> Unit) {
    while(next()) {
        block(this)
    }
}

inline fun <reified R: ResultSet> R.insert(block: (R) -> Unit) {
    moveToInsertRow()
    block(this)
    insertRow()
}

inline fun <reified R: ResultSet> R.update(block: (R) -> Unit) {
    block(this)
    updateRow()
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: Boolean?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateBoolean(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: String?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateString(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: Short?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateShort(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: Int?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateInt(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: Long?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateLong(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: Float?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateFloat(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: Double?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateDouble(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: Array<*>?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateObject(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: SQLArray?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateObject(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: SQLTime?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateTime(column, value)
    }
}

inline operator fun <reified R: ResultSet> R.set(column: String, value: SQLTimestamp?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateTimestamp(column, value)
    }
}

inline fun <reified T> ResultSet.array(column: String): Array<out T> {
    @Suppress("UNCHECKED_CAST")
    return getArray(column).array as Array<T>
}

inline fun <reified R: ResultSet> R.getNullShort(column: String): Short? {
    val s = getShort(column)
    if(wasNull())
        return null
    return s
}

inline fun <reified R: ResultSet> R.getNullInt(column: String): Int? {
    val i = getInt(column)
    if(wasNull())
        return null
    return i
}

inline fun <reified R: ResultSet> R.getNullLong(column: String): Long? {
    val l = getLong(column)
    if(wasNull())
        return null
    return l
}