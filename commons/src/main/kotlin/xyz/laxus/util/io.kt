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
@file:JvmName("IOUtil")
@file:Suppress("Unused")
package xyz.laxus.util

import java.io.File
import java.io.InputStream
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

/**
 * Gets a resource [URL] from the provided [class][KClass],
 * or `null` if there is no resource that exists with the
 * given [name].
 *
 * @param name The resource name.
 *
 * @return A resource [URL] or `null` if one doesn't exist.
 */
inline fun <reified T: Any> KClass<out T>.resourceOf(name: String): URL? = java.getResource(name)

/**
 * Gets a resource [stream][InputStream] from the provided
 * [class][KClass], or `null` if there is no resource that
 * exists with the given [name].
 *
 * @param name The resource name.
 *
 * @return A resource [stream][InputStream] or `null` if one doesn't exist.
 */
inline fun <reified T: Any> KClass<out T>.resourceStreamOf(name: String): InputStream? = java.getResourceAsStream(name)

/**
 * Returns `true` if a resource exists with the given [name].
 *
 * @param name The resource name.
 *
 * @return `true` if a resource exists with the given [name].
 */
inline fun <reified T: Any> KClass<out T>.hasResourceOf(name: String): Boolean = resourceOf(name) !== null

inline fun <reified L: ClassLoader> L.resourceOf(name: String): URL? = getResource(name)

inline fun <reified L: ClassLoader> L.resourceStreamOf(name: String): InputStream? = getResourceAsStream(name)

inline fun <reified L: ClassLoader> L.hasResourceOf(name: String): Boolean = resourceOf(name) !== null

/**
 * Generates a path based on the [first] path segment, plus any number of
 * additional path segments that are part of [more].
 *
 * @param first The first path segment.
 * @param more Additional path segments.
 *
 * @return A [Path].
 */
fun path(first: String, vararg more: String): Path {
    if(more.isEmpty()) return Paths.get(first)
    return Paths.get(first, *more)
}

/**
 * Generates a path based on the [first] path segment, plus any number of
 * additional path segments that are part of [more] and finds a [File]
 * based on that path.
 *
 * Note that the file may or may not [exist][File.exists].
 *
 * If [fromUserDir] is `true` this will start the search in the current
 * running directory.
 *
 * @param first The first path segment.
 * @param more Additional path segments.
 * @param fromUserDir Whether or not this starts in the working directory.
 *
 * @return A [File] that may or may not exist.
 */
fun file(first: String, vararg more: String, fromUserDir: Boolean = true): File {
    val path = if(fromUserDir) {
        val userDir = checkNotNull(propertyOf("user.dir")) {
            "Could not get system property 'user.dir'!"
        }
        path(userDir, first, *more)
    } else path(first, *more)

    return path.toFile()
}

/**
 * Finds a real file that [exists][File.exists] based on the [first] path
 * segment, plus any number of additional path segments that are part of [more].
 *
 * If nothing exists at the generated path, this returns `null`.
 *
 * If [fromUserDir] is `true` this will start the search in the current
 * running directory.
 *
 * @param first The first path segment.
 * @param more Additional path segments.
 * @param fromUserDir Whether or not this starts in the working directory.
 *
 * @return A [File] that exists, or `null`.
 */
fun findFile(first: String, vararg more: String, fromUserDir: Boolean = true): File? {
    return file(first, *more, fromUserDir = fromUserDir).takeIf { it.exists() }
}