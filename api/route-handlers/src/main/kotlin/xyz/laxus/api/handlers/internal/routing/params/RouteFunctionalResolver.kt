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
package xyz.laxus.api.handlers.internal.routing.params

import io.ktor.application.ApplicationCall
import xyz.laxus.util.reflect.isNullable
import kotlin.reflect.KParameter

internal class RouteFunctionalResolver<T>(
    private val parameter: KParameter,
    private val nullable: Boolean = parameter.isNullable,
    private val optional: Boolean = parameter.isOptional,
    private val handleNull: (suspend ApplicationCall.() -> Nothing)? = null,
    private val function: suspend ApplicationCall.() -> T
): RouteParamResolver<T?> {
    override suspend fun resolve(value: ApplicationCall): T? {
        val resolved = function(value)
        if(resolved === null && !nullable && !optional) {
            checkNotNull(handleNull) {
                "When resolving ${parameter.name} got a null value " +
                "(index: ${parameter.index}, type: ${parameter.type})"
            }.invoke(value)
        }
        return resolved
    }
}