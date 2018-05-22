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
@file:Suppress("Unused")
@file:JvmName("HoconUtil")
package xyz.laxus.util

import com.typesafe.config.*
import xyz.laxus.util.reflect.loadClass
import xyz.laxus.util.reflect.valueOf
import kotlin.reflect.KClass

fun loadConfig(name: String): Config = ConfigFactory.load(name)

fun Config.config(path: String): Config? = ifValid(path, ::getConfig)
fun Config.obj(path: String): ConfigObject? = ifValid(path, ::getObject)
fun Config.list(path: String): ConfigList? = ifValid(path, ::getList)
fun Config.string(path: String): String? = ifValid(path, ::getString)
fun Config.boolean(path: String): Boolean? = ifValid(path, ::getBoolean)
fun Config.int(path: String): Int? = ifValid(path, ::getInt)
fun Config.long(path: String): Long? = ifValid(path, ::getLong)
fun Config.double(path: String): Double? = ifValid(path, ::getDouble)
fun Config.klass(path: String): KClass<*>? = string(path)?.let { loadClass(it) }
inline fun <reified E: Enum<E>> Config.enum(path: String): E? = string(path)?.let {
    ignored(null) { E::class.valueOf(it) }
}

private inline fun <reified T> Config.ifValid(path: String, block: (String) -> T): T? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return block(path)
}

fun ConfigObject.string(key: String): String? = this[key]?.string
fun ConfigObject.short(key: String): Short? = this[key]?.short
fun ConfigObject.int(key: String): Int? = this[key]?.int
fun ConfigObject.long(key: String): Long? = this[key]?.long
fun ConfigObject.float(key: String): Float? = this[key]?.float
fun ConfigObject.double(key: String): Double? = this[key]?.double
fun ConfigObject.klass(key: String): KClass<*>? = this[key]?.klass
fun ConfigObject.obj(key: String): ConfigObject? = toConfig().obj(key)
fun ConfigObject.config(key: String): Config? = toConfig().config(key)
fun ConfigObject.list(key: String): ConfigList? = toConfig().list(key)
fun ConfigObject.value(key: String): ConfigValue? = get(key)
inline fun <reified E: Enum<E>> ConfigObject.enum(key: String): E? = string(key)?.let {
    ignored(null) { E::class.valueOf(it) }
}

val ConfigValue.string: String? get() = unwrapped()?.let { it as? String ?: it.toString() }
val ConfigValue.short: Short? get() = unwrapped()?.let { it as? Number }?.toShort()
val ConfigValue.int: Int? get() = unwrapped()?.let { it as? Number }?.toInt()
val ConfigValue.long: Long? get() = unwrapped()?.let { it as? Number }?.toLong()
val ConfigValue.float: Float? get() = unwrapped()?.let { it as? Number }?.toFloat()
val ConfigValue.double: Double? get() = unwrapped()?.let { it as? Number }?.toDouble()
val ConfigValue.klass: KClass<*>? get() =  string?.let { loadClass(it) }
inline fun <reified E: Enum<E>> ConfigValue.enum(): E? = string?.let {
    ignored(null) { E::class.valueOf(it) }
}

fun emptyConfig(): Config = ConfigFactory.empty()