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
@file:JvmName("SystemUtil")
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
val currentTime get() = System.currentTimeMillis()

/**
 * Gets a [system property][System.getProperty] mapped to
 * the provided [key], or `null` if one doesn't match.
 */
fun propertyOf(key: String): String? = System.getProperty(key)

/**
 * Gets the current [Runtime].
 */
val runtime: Runtime get() = Runtime.getRuntime()

/**
 * Gets the [Runtime's][Runtime] [total memory][Runtime.totalMemory].
 */
inline val Runtime.totalMemory inline get() = totalMemory()

/**
 * Gets the [Runtime's][Runtime] [free memory][Runtime.freeMemory].
 */
inline val Runtime.freeMemory inline get() = freeMemory()

/**
 * Gets the [Runtime's][Runtime] [max memory][Runtime.maxMemory].
 */
inline val Runtime.maxMemory inline get() = maxMemory()

/**
 * Gets a snapshot [Runtime's][Runtime] [Memory].
 */
val Runtime.memory get() = Memory(maxMemory, freeMemory, totalMemory)

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

/** Runtime snapshot of Memory statistics. */
data class Memory internal constructor(
    /** The runtime's maximum memory. */
    val max: Long,
    /** The runtime's free memory. */
    val free: Long,
    /** The runtime's total memory. */
    val total: Long
)