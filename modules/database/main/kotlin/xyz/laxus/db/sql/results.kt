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

import xyz.laxus.db.sql.SQLArrayType.*
import java.sql.ResultSet
import kotlin.reflect.KClass
import kotlin.reflect.full.cast

inline fun ResultSet.insert(block: (ResultSet) -> Unit) {
    moveToInsertRow()
    block(this)
    insertRow()
}

inline fun ResultSet.update(block: (ResultSet) -> Unit) {
    block(this)
    updateRow()
}

operator fun ResultSet.set(column: String, value: Boolean?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateBoolean(column, value)
    }
}

operator fun ResultSet.set(column: String, value: String?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateString(column, value)
    }
}

operator fun ResultSet.set(column: String, value: Short?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateShort(column, value)
    }
}

operator fun ResultSet.set(column: String, value: Int?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateInt(column, value)
    }
}

operator fun ResultSet.set(column: String, value: Long?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateLong(column, value)
    }
}

operator fun ResultSet.set(column: String, value: Float?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateFloat(column, value)
    }
}

operator fun ResultSet.set(column: String, value: Double?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateDouble(column, value)
    }
}

operator fun ResultSet.set(column: String, value: Array<String>?) {
    this[column, VARCHAR] = value
}

operator fun ResultSet.set(column: String, value: BooleanArray?) {
    this[column, BOOL] = value?.toTypedArray()
}

operator fun ResultSet.set(column: String, value: ShortArray?) {
    this[column, INT_2] = value?.toTypedArray()
}

operator fun ResultSet.set(column: String, value: IntArray?) {
    this[column, INT_4] = value?.toTypedArray()
}

operator fun ResultSet.set(column: String, value: LongArray?) {
    this[column, INT_8] = value?.toTypedArray()
}

operator fun ResultSet.set(column: String, value: FloatArray?) {
    this[column, FLOAT_4] = value?.toTypedArray()
}

operator fun ResultSet.set(column: String, value: DoubleArray?) {
    this[column, FLOAT_8] = value?.toTypedArray()
}

operator fun ResultSet.set(column: String, type: SQLArrayType, value: Array<*>?) {
    if(value === null) {
        updateNull(column)
    } else {
        val conn = requireNotNull(statement?.connection) { "ResultSet does not support statement retrieval!" }
        this[column] = conn.createArrayOf(type.serverName, value)
    }
}

operator fun ResultSet.set(column: String, value: SQLDate?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateDate(column, value)
    }
}

operator fun ResultSet.set(column: String, value: SQLArray?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateArray(column, value)
    }
}

operator fun ResultSet.set(column: String, value: SQLTime?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateTime(column, value)
    }
}

operator fun ResultSet.set(column: String, value: SQLTimestamp?) {
    if(value === null) {
        updateNull(column)
    } else {
        updateTimestamp(column, value)
    }
}

inline fun <reified T: Any> ResultSet.array(column: String) = array(column, T::class)

fun <T: Any> ResultSet.array(column: String, klass: KClass<T>): Array<T> {
    val array = getArray(column).array as? Array<*>
    @Suppress("UNCHECKED_CAST")
    return (array?.let {
        Array<Any>(array.size) { klass.cast(array[it]) }
    } ?: emptyArray()) as Array<T>
}

fun ResultSet.stringArray(column: String): Array<String> {
    val array = getArray(column).array
    return when(array) {
        null -> emptyArray()
        is Array<*> -> Array(array.size) { i ->
            val value = array[i]?.let { (it as? String ?: it.toString()) }
            requireNotNull(value)
        }
        else -> emptyArray()
    }
}

fun ResultSet.boolArray(column: String): BooleanArray {
    val array = getArray(column).array
    return when(array) {
        null -> booleanArrayOf()
        is BooleanArray -> array
        is Array<*> -> BooleanArray(array.size) { i ->
            val value = array[i]?.let { (it as? Boolean ?: it.toString().toBoolean()) }
            requireNotNull(value)
        }
        else -> booleanArrayOf()
    }
}

fun ResultSet.shortArray(column: String): ShortArray {
    val array = getArray(column).array
    return when(array) {
        null -> shortArrayOf()
        is ShortArray -> array
        is Array<*> -> ShortArray(array.size) { i ->
            val value = array[i]?.let { (it as? Short ?: it.toString().toShortOrNull()) }
            requireNotNull(value)
        }
        else -> shortArrayOf()
    }
}

fun ResultSet.intArray(column: String): IntArray {
    val array = getArray(column).array
    return when(array) {
        null -> intArrayOf()
        is IntArray -> array
        is Array<*> -> IntArray(array.size) { i ->
            val value = array[i]?.let { (it as? Int ?: it.toString().toIntOrNull()) }
            requireNotNull(value)
        }
        else -> intArrayOf()
    }
}

fun ResultSet.longArray(column: String): LongArray {
    val array = getArray(column).array
    return when(array) {
        null -> longArrayOf()
        is LongArray -> array
        is Array<*> -> LongArray(array.size) { i ->
            val value = array[i]?.let { (it as? Long ?: it.toString().toLongOrNull()) }
            requireNotNull(value)
        }
        else -> longArrayOf()
    }
}

fun ResultSet.floatArray(column: String): FloatArray {
    val array = getArray(column).array
    return when(array) {
        null -> floatArrayOf()
        is FloatArray -> array
        is Array<*> -> FloatArray(array.size) { i ->
            val value = array[i]?.let { (it as? Float ?: it.toString().toFloatOrNull()) }
            requireNotNull(value)
        }
        else -> floatArrayOf()
    }
}

fun ResultSet.doubleArray(column: String): DoubleArray {
    val array = getArray(column).array
    return when(array) {
        null -> doubleArrayOf()
        is DoubleArray -> array
        is Array<*> -> DoubleArray(array.size) { i ->
            val value = array[i]?.let { (it as? Double ?: it.toString().toDoubleOrNull()) }
            requireNotNull(value)
        }
        else -> doubleArrayOf()
    }
}

fun ResultSet.getNullShort(column: String): Short? {
    val s = getShort(column)
    if(wasNull())
        return null
    return s
}

fun ResultSet.getNullInt(column: String): Int? {
    val i = getInt(column)
    if(wasNull())
        return null
    return i
}

fun ResultSet.getNullLong(column: String): Long? {
    val l = getLong(column)
    if(wasNull())
        return null
    return l
}
