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

import kotlin.concurrent.thread

/**
 * Gets the system's [line separator][System.lineSeparator].
 */
val lineSeparator: String get() = System.lineSeparator()

/**
 * Gets the system's [current time][System.currentTimeMillis] (in milliseconds).
 */
val currentTime: Long get() = System.currentTimeMillis()

/**
 * Gets the current [Runtime].
 */
val runtime: Runtime get() = Runtime.getRuntime()

/**
 * Gets the [Runtime's][Runtime] [total memory][Runtime.totalMemory].
 */
val Runtime.totalMemory: Long get() = totalMemory()

/**
 * Gets the [Runtime's][Runtime] [free memory][Runtime.freeMemory].
 */
val Runtime.freeMemory: Long get() = freeMemory()

/**
 * Gets the [Runtime's][Runtime] [max memory][Runtime.maxMemory].
 */
val Runtime.maxMemory: Long get() = maxMemory()

/**
 * Gets a [system property][System.getProperty] mapped to
 * the provided [key], or `null` if one doesn't match.
 */
fun propertyOf(key: String): String? = System.getProperty(key)

/**
 * Adds a [shutdown hook][Runtime.addShutdownHook]
 * to the JVM [Runtime].
 */
fun onJvmShutdown(thread: Thread) = runtime.addShutdownHook(thread)

/**
 * Creates and adds a [shutdown hook][Runtime.addShutdownHook]
 * to the JVM [Runtime].
 *
 * This creates a new thread with the provided [name] that
 * executes the provided [block].
 */
fun onJvmShutdown(name: String, block: () -> Unit) {
    onJvmShutdown(thread(name = name, start = false, isDaemon = true, block = block))
}
