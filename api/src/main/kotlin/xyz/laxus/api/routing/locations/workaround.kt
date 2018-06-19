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
@file:Suppress("Deprecation")
package xyz.laxus.api.routing.locations

import io.ktor.pipeline.ContextDsl
import io.ktor.routing.Route
import io.ktor.routing.Routing
import io.ktor.locations.location as ktorLocation

@ContextDsl inline fun <reified T: Any> Routing.location(noinline block: Route.() -> Unit) = ktorLocation<T>(block)
@ContextDsl inline fun <reified T: Any> Route.location(noinline block: Route.() -> Unit) = ktorLocation<T>(block)