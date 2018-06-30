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
package xyz.laxus.api.handlers.internal

import io.ktor.config.ApplicationConfig
import xyz.laxus.api.handlers.annotations.Configuration
import xyz.laxus.api.handlers.annotations.Property
import xyz.laxus.api.handlers.internal.reflect.ParamType
import xyz.laxus.api.handlers.internal.reflect.runInvocationSafe
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure

internal class ConfiguredConstructorFunction<T>(
    private val constructor: KFunction<T>,
    configuration: Configuration
) {
    private val resolvers: Map<KParameter, (ApplicationConfig) -> Any?>

    init {
        val root = configuration.root
        this.resolvers = constructor.parameters.associateBy({ it }) resolvers@ { parameter ->
            val path = root + '.' + checkNotNull(parameter.findAnnotation<Property>()?.path ?: parameter.name)

            val optional = parameter.isOptional
            val type = parameter.type
            val isList = type.jvmErasure === List::class

            val conversionType = if(!isList) type else {
                val typeArguments = type.arguments
                when(typeArguments.size) {
                    1 -> typeArguments[0].type ?: Any::class.createType(nullable = true)
                    else -> throw IllegalArgumentException("Cannot determine type argument of $parameter")
                }
            }

            val paramType = requireNotNull(ParamType.from(conversionType)) {
                "Cannot properly compute argument type '$type' for constructor $constructor"
            }

            val nullable = conversionType.isMarkedNullable

            if(isList) {
                return@resolvers resolverFunction {
                    val property = it.property(path)
                    if(nullable) {
                        property.getList().map { paramType.convert(it) }
                    } else {
                        property.getList().mapNotNull { paramType.convert(it) }
                    }
                }
            }

            return@resolvers resolverFunction f@ {
                val converted = paramType.convert(it.property(path).getString())
                if(converted === null) {
                    if(optional) {
                        return@f Fake
                    }
                    require(nullable) { "Received null value for non-null parameter: $parameter" }
                }
                return@f converted
            }
        }
    }

    fun construct(config: ApplicationConfig, parentInst: Any?): T {
        val seq = resolvers.asSequence().map { it.key to it.value(config) }
        val instParam = constructor.instanceParameter
        val treatAsNested = parentInst !== null && instParam !== null
        val resolvers = if(treatAsNested) { seq + (instParam!! to parentInst) } else seq
        val values = resolvers.filter { it.second !== Fake }.toMap()
        return runInvocationSafe { constructor.callBy(values) }
    }

    private companion object Fake {
        private fun resolverFunction(function: (ApplicationConfig) -> Any?) = function
    }
}