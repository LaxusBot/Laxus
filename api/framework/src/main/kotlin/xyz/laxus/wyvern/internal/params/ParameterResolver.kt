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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.laxus.wyvern.internal.params

import xyz.laxus.wyvern.http.CallContext
import xyz.laxus.util.reflect.isNullable
import kotlin.reflect.KParameter

/**
 * @author Kaidan Gustave
 */
internal class ParameterResolver<T>(
    val parameter: KParameter,
    val nullable: Boolean = parameter.isNullable,
    val optional: Boolean = parameter.isOptional,
    private val handleNull: (CallContext.() -> Nothing)? = null,
    private val resolve: CallContext.() -> T
) {
    operator fun invoke(context: CallContext): T {
        val resolved = resolve(context)
        if(resolved === null && !nullable && !optional) {
            checkNotNull(handleNull) {
                "When resolving ${parameter.name} got a null value " +
                "(index: ${parameter.index}, type: ${parameter.type})"
            }.invoke(context)
        }
        return resolved
    }
}