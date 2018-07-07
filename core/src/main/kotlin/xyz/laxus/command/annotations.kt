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
@file:JvmName("CommandAnnotations")
package xyz.laxus.command

import xyz.laxus.db.entities.ExperimentAccess.Level

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AutoCooldown(val mode: AutoCooldownMode = AutoCooldownMode.AFTER)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MustHaveArguments(val error: String = "")

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Suppress("ANNOTATION_CLASS_MEMBER")
annotation class ExperimentalCommand(
    val info: String = "",
    val level: Level = Level.OPEN_BETA
)

enum class AutoCooldownMode { OFF, BEFORE, AFTER }
