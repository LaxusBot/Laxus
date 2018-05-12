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
package xyz.laxus.api.internal

import spark.route.HttpMethod
import xyz.laxus.api.internal.context.ContextDsl
import xyz.laxus.api.internal.context.RouteContext
import xyz.laxus.api.service

internal typealias RouteHandle = suspend RouteContext.() -> Unit

@ContextDsl
internal fun path(path: String, group: () -> Unit) {
    service.path(path, group)
}

@ContextDsl
internal fun get(path: String, handle: RouteHandle) {
    service.addRoute(HttpMethod.get, SuspendedRoute(path, handle))
}

@ContextDsl
internal fun post(path: String, handle: RouteHandle) {
    service.addRoute(HttpMethod.post, SuspendedRoute(path, handle))
}

@ContextDsl
internal fun delete(path: String, handle: RouteHandle) {
    service.addRoute(HttpMethod.delete, SuspendedRoute(path, handle))
}