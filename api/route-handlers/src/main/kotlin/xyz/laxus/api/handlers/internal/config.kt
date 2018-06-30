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
package xyz.laxus.api.handlers.internal

import io.ktor.config.ApplicationConfigValue as Value
import xyz.laxus.util.reflect.loadClass
import xyz.laxus.util.reflect.valueOf
import kotlin.reflect.KClass

internal fun Value.getInt() = getString().toIntOrNull()
internal fun Value.getIntList() = getList().mapNotNull { it.toIntOrNull() }
internal fun Value.getLong() = getString().toLongOrNull()
internal fun Value.getLongList() = getList().mapNotNull { it.toLongOrNull() }
internal fun Value.getDouble() = getString().toDoubleOrNull()
internal fun Value.getDoubleList() = getList().mapNotNull { it.toDoubleOrNull() }
internal fun Value.getFloat() = getString().toFloatOrNull()
internal fun Value.getFloatList() = getList().mapNotNull { it.toFloatOrNull() }

internal fun Value.getClass(classLoader: ClassLoader? = null) = loadClass(getString(), classLoader)
internal fun Value.getClassList(classLoader: ClassLoader? = null) = getList().mapNotNull { loadClass(getString(), classLoader) }

internal fun <E: Enum<E>> Value.getEnum(klass: KClass<E>) = klass.valueOf(getString())
internal fun <E: Enum<E>> Value.getEnumList(klass: KClass<E>) = getList().map { klass.valueOf(getString()) }