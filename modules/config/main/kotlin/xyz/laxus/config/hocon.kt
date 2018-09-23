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
@file:JvmName("HoconUtil")
@file:Suppress("Unused")
package xyz.laxus.config

import com.typesafe.config.*
import xyz.laxus.reflect.loadClass
import xyz.laxus.reflect.valueOf
import xyz.laxus.utils.resourceOf
import java.net.URL
import kotlin.reflect.KClass

fun loadConfig(name: String): Config {
    val url = checkNotNull(Any::class.resourceOf(name))
    return loadConfig(url)
}
fun loadConfig(url: URL): Config = ConfigFactory.parseURL(url)

fun Config.config(path: String): Config = ifValid(path, ::getConfig)
fun Config.obj(path: String): ConfigObject = ifValid(path, ::getObject)
fun Config.list(path: String): ConfigList = ifValid(path, ::getList)
fun Config.string(path: String): String = ifValid(path, ::getString)
fun Config.boolean(path: String): Boolean = ifValid(path, ::getBoolean)
fun Config.int(path: String): Int = ifValid(path, ::getInt)
fun Config.long(path: String): Long = ifValid(path, ::getLong)
fun Config.double(path: String): Double = ifValid(path, ::getDouble)

fun Config.nullConfig(path: String): Config? = ifValidOrNull(path, ::getConfig)
fun Config.nullObj(path: String): ConfigObject? = ifValidOrNull(path, ::getObject)
fun Config.nullList(path: String): ConfigList? = ifValidOrNull(path, ::getList)
fun Config.nullString(path: String): String? = ifValidOrNull(path, ::getString)
fun Config.nullBoolean(path: String): Boolean? = ifValidOrNull(path, ::getBoolean)
fun Config.nullInt(path: String): Int? = ifValidOrNull(path, ::getInt)
fun Config.nullLong(path: String): Long? = ifValidOrNull(path, ::getLong)
fun Config.nullDouble(path: String): Double? = ifValidOrNull(path, ::getDouble)
fun Config.nullValue(path: String): ConfigValue? = ifValidOrNull(path, ::getValue)

fun Config.klass(path: String): KClass<*>? = loadClass(string(path))
inline fun <reified E: Enum<E>> Config.enum(path: String): E? =
    runCatching<E?> { E::class.valueOf(string(path)) }.getOrNull()

private inline fun <reified T: Any> Config.ifValidOrNull(path: String, block: (String) -> T): T? {
    if(!hasPath(path) || getIsNull(path))
        return null
    return block(path)
}

private inline fun <reified T> Config.ifValid(path: String, block: (String) -> T): T {
    require(hasPath(path)) { "Value with path '$path' does not exist!" }
    require(!getIsNull(path)) { "Value with path '$path' is null!" }
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
inline fun <reified E: Enum<E>> ConfigObject.enum(key: String): E? =
    string(key)?.let { runCatching<E?> { E::class.valueOf(it) }.getOrNull() }

val ConfigValue.string: String? get() = unwrapped()?.let { it as? String ?: it.toString() }
val ConfigValue.short: Short? get() = unwrapped()?.let { it as? Number }?.toShort()
val ConfigValue.int: Int? get() = unwrapped()?.let { it as? Number }?.toInt()
val ConfigValue.long: Long? get() = unwrapped()?.let { it as? Number }?.toLong()
val ConfigValue.float: Float? get() = unwrapped()?.let { it as? Number }?.toFloat()
val ConfigValue.double: Double? get() = unwrapped()?.let { it as? Number }?.toDouble()
val ConfigValue.klass: KClass<*>? get() =  string?.let { loadClass(it) }
inline fun <reified E: Enum<E>> ConfigValue.enum(): E? =
    string?.let { runCatching<E?> { E::class.valueOf(it) }.getOrNull() }

fun emptyConfig(): Config = ConfigFactory.empty()
