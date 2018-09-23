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
@file:JvmName("LoadingUtil")
package xyz.laxus.reflect

import kotlin.reflect.KClass
import java.lang.ClassNotFoundException as CNFException

fun loadClass(name: String): KClass<*>? {
    return try { Class.forName(name).kotlin } catch(e: CNFException) { null }
}

/**
 * Uses the receiver [ClassLoader] to load a [KClass]
 * with the provided fully-qualified name.
 *
 * @receiver The [ClassLoader] to use.
 * @param fqn The fully-qualified name of the [KClass] to load.
 *
 * @return The loaded [KClass] or `null` if one with the [fqn] could not be found.
 */
fun ClassLoader.loadKlass(fqn: String): KClass<*>? {
    return try { this.loadClass(fqn)?.kotlin } catch(ex: CNFException) { null }
}
