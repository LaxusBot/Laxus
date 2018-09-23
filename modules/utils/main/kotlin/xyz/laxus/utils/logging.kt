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
@file:JvmName("LoggingUtil")
@file:Suppress("Unused")
package xyz.laxus.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * Creates a [Logger] with the provided [name]
 * using slf4j's [LoggerFactory].
 *
 * @param name The name of the [Logger].
 *
 * @return A [Logger] with the provided [name].
 */
fun createLogger(name: String): Logger = LoggerFactory.getLogger(name)

/**
 * Creates a [Logger] with the provided [class][KClass]
 * using slf4j's [LoggerFactory].
 *
 * @param klazz The [class][KClass] of the [Logger].
 *
 * @return A [Logger] with the provided [class][KClass].
 */
fun createLogger(klazz: KClass<*>): Logger = LoggerFactory.getLogger(klazz.java)

/**
 * Log a message at INFO level.
 *
 * @param msg the message string to be logged
 */
inline fun Logger.info(msg: () -> String) = info(msg())

/**
 * Log a message at WARN level.
 *
 * @param msg the message string to be logged
 */
inline fun Logger.warn(msg: () -> String) = warn(msg())

/**
 * Log a message at ERROR level.
 *
 * @param msg the message string to be logged
 */
inline fun Logger.error(msg: () -> String) = error(msg())

/**
 * Log a message at DEBUG level.
 *
 * @param msg the message string to be logged
 */
inline fun Logger.debug(msg: () -> String) = debug(msg())

/**
 * Log a message at TRACE level.
 *
 * @param msg the message string to be logged
 */
inline fun Logger.trace(msg: () -> String) = trace(msg())
