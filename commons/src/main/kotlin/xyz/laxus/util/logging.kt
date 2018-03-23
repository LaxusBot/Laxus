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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

fun createLogger(name: String): Logger = LoggerFactory.getLogger(name)
fun <T: Any> createLogger(klazz: KClass<T>): Logger = LoggerFactory.getLogger(klazz.java)

inline fun Logger.info(block: () -> String) = info(block())
inline fun Logger.warn(block: () -> String) = warn(block())
inline fun Logger.error(block: () -> String) = error(block())
inline fun Logger.debug(block: () -> String) = debug(block())
inline fun Logger.trace(block: () -> String) = trace(block())
