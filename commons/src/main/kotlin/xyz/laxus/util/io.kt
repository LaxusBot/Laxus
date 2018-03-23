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
package xyz.laxus.util

import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

inline fun <reified T: Any> KClass<out T>.resourceOf(name: String): URL? = java.getResource(name)
inline fun <reified T: Any> KClass<out T>.resourceStreamOf(name: String): InputStream? = java.getResourceAsStream(name)
inline fun <reified T: Any> KClass<out T>.hasResourceOf(name: String): Boolean = resourceOf(name) !== null

fun file(first: String, vararg more: String, fromUserDir: Boolean = true): File {
    val path = if(fromUserDir) {
        val userDir = checkNotNull(propertyOf("user.dir")) {
            "Could not get system property 'user.dir'!"
        }
        path(userDir, first, *more)
    } else path(first, *more)

    return path.toFile()
}

fun path(first: String, vararg more: String): Path {
    if(more.isEmpty()) return Paths.get(first)
    return Paths.get(first, *more)
}

fun findFile(first: String, vararg more: String, fromUserDir: Boolean = true): File? {
    return file(first, *more, fromUserDir = fromUserDir).takeIf { it.exists() }
}