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
@file:Suppress("LiftReturnOrAssignment")
package xyz.laxus.wyvern.internal

import xyz.laxus.wyvern.API
import xyz.laxus.wyvern.http.HttpMethod
import kotlin.reflect.KFunction

internal class RouteRunner(
    private val method: HttpMethod,
    private val path: String,
    api: API,
    base: Any,
    function: KFunction<*>,
    headers: Map<String, String>
): MappedCallFunction(api, base, function, headers) {
    internal fun generate() {
        val route = when {
            function.isSuspend -> SuspendedRoute(api, path, handleSuspended)
            else -> NormalRoute(api, path, handle)
        }

        api.newRoute(method, route)
    }
}
