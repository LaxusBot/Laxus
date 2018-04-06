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
package xyz.laxus.util

import com.typesafe.config.*
import xyz.laxus.util.reflect.loadClass
import kotlin.reflect.KClass

fun loadConfig(name: String): Config {
    return ConfigFactory.load(name)
}

fun Config.config(path: String): Config? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return getConfig(path)
}

fun Config.obj(path: String): ConfigObject? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return getObject(path)
}

fun Config.list(path: String): ConfigList? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return getList(path)
}

fun Config.string(path: String): String? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return getString(path)
}

fun Config.boolean(path: String): Boolean? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return getBoolean(path)
}

fun Config.int(path: String): Int? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return getInt(path)
}

fun Config.long(path: String): Long? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return getLong(path)
}

fun Config.klass(path: String): KClass<*>? = string(path)?.let { loadClass(it) }

fun <E: Enum<E>> Config.enum(path: String, type: KClass<E>): E? {
    return string(path)?.let { enum ->
        type.java.enumConstants.firstOrNull { it.name.equals(enum, ignoreCase = true) }
    }
}

inline fun <reified E: Enum<E>> Config.enum(path: String): E? = enum(path, E::class)

fun ConfigObject.string(key: String): String? = this[key]?.string
fun ConfigObject.short(key: String): Short? = this[key]?.short
fun ConfigObject.int(key: String): Int? = this[key]?.int
fun ConfigObject.long(key: String): Long? = this[key]?.long
fun ConfigObject.float(key: String): Float? = this[key]?.float
fun ConfigObject.double(key: String): Double? = this[key]?.double
fun ConfigObject.klass(key: String): KClass<*>? = this[key]?.klass
fun <E: Enum<E>> ConfigObject.enum(key: String, type: KClass<E>): E? = string(key)?.let { enum ->
    type.java.enumConstants.firstOrNull { it.name.equals(enum, ignoreCase = true) }
}
inline fun <reified E: Enum<E>> ConfigObject.enum(key: String): E? = enum(key, E::class)
fun ConfigObject.obj(key: String): ConfigObject? = toConfig().obj(key)
fun ConfigObject.config(key: String): Config? = toConfig().config(key)
fun ConfigObject.list(key: String): ConfigList? = toConfig().list(key)

val ConfigValue.string: String? get() = unwrapped()?.toString()
val ConfigValue.short: Short? get() = unwrapped()?.let { it as? Number }?.toShort()
val ConfigValue.int: Int? get() = unwrapped()?.let { it as? Number }?.toInt()
val ConfigValue.long: Long? get() = unwrapped()?.let { it as? Number }?.toLong()
val ConfigValue.float: Float? get() = unwrapped()?.let { it as? Number }?.toFloat()
val ConfigValue.double: Double? get() = unwrapped()?.let { it as? Number }?.toDouble()
val ConfigValue.klass: KClass<*>? get() = string?.let { loadClass(it) }