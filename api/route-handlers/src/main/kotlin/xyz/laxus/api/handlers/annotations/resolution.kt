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
package xyz.laxus.api.handlers.annotations

import xyz.laxus.api.handlers.internal.reflect.ParamMapped
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

@ParamMapped
@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Body

@ParamMapped
@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Param(val value: String = "")

@ParamMapped
@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class QueryParam(val value: String = "")

@ParamMapped
@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Header(val value: String = "")

@ParamMapped
@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Locate

@Retention(RUNTIME)
@Target(VALUE_PARAMETER)
annotation class Default(val value: String)
