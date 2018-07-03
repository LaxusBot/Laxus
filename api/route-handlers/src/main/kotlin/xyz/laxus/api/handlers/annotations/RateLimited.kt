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

import java.util.concurrent.TimeUnit
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*

@Retention(RUNTIME)
@Target(CLASS, FUNCTION)
annotation class RateLimited(
    val limit: Int,
    val reset: Long,
    val unit: TimeUnit = TimeUnit.SECONDS
)
