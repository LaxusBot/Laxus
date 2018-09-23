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
package xyz.laxus.db.annotation

import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

/**
 * Marks a [table object][xyz.laxus.db.Table]
 * to use the provided [value] as it's name.
 */
@Retention(RUNTIME)
@Target(CLASS)
annotation class TableName(val value: String)

/**
 * An annotation representing a column
 * on a [table][xyz.laxus.db.Table].
 */
@Repeatable
@Retention(RUNTIME)
@Target(CLASS)
annotation class Column(
    val name: String,
    val type: String,
    val nullable: Boolean = false,
    val primary: Boolean = false,
    val def: String = ""
)

/**
 * Multi-annotation for [Column].
 */
@Retention(RUNTIME)
@Target(CLASS)
annotation class Columns(vararg val value: Column)

/**
 * Automatically registers all [Column] annotations
 * on the same [table object][xyz.laxus.db.Table]
 * as [primary keys][Column.primary].
 */
@Retention(RUNTIME)
@Target(CLASS)
annotation class AllPrimary