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
