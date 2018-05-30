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
package xyz.laxus.wyvern.annotation

/**
 * Annotation declaring the default value of a parameter for
 * a [Handle] function.
 *
 * This is a temporary fix for suspend functions with default
 * values, as default values are not yet supported by Kotlin.
 *
 * Also be aware that only [query param][QueryParam] and
 * [header][Header] arguments are supported!
 *
 * See: [https://youtrack.jetbrains.net/issue/KT-21972]
 *
 * @author Kaidan Gustave
 */

// Please bump the issue above if you want to see this behavior
//streamlined! It is unfortunate that I cannot implement actual
//kotlin-like behavior because one silly function doesn't exist
//at this moment in time!

@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Default(val value: String)
