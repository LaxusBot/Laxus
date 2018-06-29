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
package xyz.laxus.api.handlers.annotations

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*

@PathExtension
@Target(FUNCTION)
@Retention(RUNTIME)
@Method("GET")
annotation class Get(@PathExtension val value: String = "")

@PathExtension
@Target(FUNCTION)
@Retention(RUNTIME)
@Method("POST")
annotation class Post(@PathExtension val value: String = "")

@PathExtension
@Target(FUNCTION)
@Retention(RUNTIME)
@Method("PUT")
annotation class Put(@PathExtension val value: String = "")

@PathExtension
@Target(FUNCTION)
@Retention(RUNTIME)
@Method("PATCH")
annotation class Patch(@PathExtension val value: String = "")

@PathExtension
@Target(FUNCTION)
@Retention(RUNTIME)
@Method("DELETE")
annotation class Delete(@PathExtension val value: String = "")

@PathExtension
@Target(FUNCTION)
@Retention(RUNTIME)
@Method("HEAD")
annotation class Head(@PathExtension val value: String = "")

@PathExtension
@Target(FUNCTION)
@Retention(RUNTIME)
@Method("OPTIONS")
annotation class Options(@PathExtension val value: String = "")

@PathExtension
@Retention(RUNTIME)
@Target(ANNOTATION_CLASS)
annotation class Method(val value: String, @PathExtension val path: String = "")

/**
 * Represents an annotation that can have an extendable
 */
@Retention(RUNTIME)
@Target(VALUE_PARAMETER, ANNOTATION_CLASS)
annotation class PathExtension
