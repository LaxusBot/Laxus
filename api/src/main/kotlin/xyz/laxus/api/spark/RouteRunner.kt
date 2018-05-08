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
package xyz.laxus.api.spark

import spark.route.HttpMethod
import xyz.laxus.api.spark.annotation.*
import xyz.laxus.api.spark.context.RouteContext
import xyz.laxus.api.util.ContentType
import xyz.laxus.util.modifyIf
import xyz.laxus.util.reflect.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.instanceParameter

/**
 * @author Kaidan Gustave
 */
class RouteRunner(
    private val path: String,
    private val method: HttpMethod,
    private val base: Any,
    private val function: KFunction<*>
) {
    private val paramResolvers = arrayListOf<ParameterResolver<*>>()
    private val handle: suspend RouteContext.() -> Unit = {
        val parameterValues = resolveParams()
        if(function.isSuspend) {
            function.callSuspended(base, *parameterValues)
        } else {
            function.call(base, *parameterValues)
        }
    }

    init {
        initMappedParams()
    }

    private suspend fun RouteContext.resolveParams(): Array<Any?> {
        if(paramResolvers.isEmpty()) return emptyArray()
        return paramResolvers.map { it(this) }.toTypedArray()
    }

    private fun initMappedParams() {
        val params = function.parameters.toMutableList()

        params.remove(function.instanceParameter)

        if(!function.isStatic) {
            // remove instance parameter
            params.removeAt(0)
        }

        // Immediately map extension parameter
        if(function.isExtension) {
            val extParam = checkNotNull(function.extensionReceiverParameter) {
                "Expected not-null extension receiver parameter!"
            }
            params.remove(extParam)
            paramResolvers += mapExtension(extParam)
        }

        if(params.isEmpty()) // Only the minimum parameters
            return

        params.forEach {
            check(it.hasValidAnnotation()) {
                "Parameter ${it.name} is invalid (index: ${it.index}, type: ${it.type}, " +
                "signature: ${base::class}#${function.name})"
            }

            paramResolvers += when {
                it.hasAnnotation<Header>() -> mapHeader(it)
                it.hasAnnotation<Param>() -> mapParam(it)
                it.hasAnnotation<QueryParam>() -> mapQueryParam(it)

                else -> throw IllegalStateException("Unable to map param '$it' for function " +
                                                    "'$function' in handler '${base::class}'")
            }
        }
    }

    private fun mapExtension(parameter: KParameter): ParameterResolver<RouteContext> {
        check(parameter.isType<RouteContext>()) { "Extension receiver must have the type: ${RouteContext::class}!" }
        check(!parameter.isNullable) { "Extension receiver may not be nullable!" }
        // Extension param cannot be null with framework
        return ParameterResolver(parameter, false) { this }
    }

    private fun mapHeader(parameter: KParameter): ParameterResolver<*> {
        val header = checkNotNull(parameter.findAnnotation<Header>()) { "Expected @Header was null!" }
        val headerKey = header.value
        val isContentType = headerKey.equals("Content-Type", true)
        val nullable = parameter.isType<String?>() || (isContentType && parameter.isType<ContentType?>())
        return ParameterResolver(parameter, nullable) resolve@ {
            if(isContentType) {
                return@resolve request.contentType
            } else {
                return@resolve request.header(headerKey)
            }
        }
    }

    private fun mapParam(parameter: KParameter): ParameterResolver<*> {
        val param = checkNotNull(parameter.findAnnotation<Param>()) { "Expected @Param was null!" }
        val paramKey = param.value.modifyIf({ !it.startsWith(':') }) { it.padStart(1, ':') }
        val paramType = param.type

        check(paramType.assertTypeMatch(parameter.type))

        val nullable = parameter.isNullable
        return ParameterResolver(parameter, nullable) resolve@ {
            val paramValue = request.params[paramKey]
            if(paramValue === null) {
                return@resolve null
            }

            when(paramType) {
                ParamType.STRING -> return@resolve paramValue
                ParamType.INT -> return@resolve paramValue.toInt()
                ParamType.LONG -> return@resolve paramValue.toLong()
            }
        }
    }

    private fun mapQueryParam(parameter: KParameter): ParameterResolver<*> {
        val param = checkNotNull(parameter.findAnnotation<QueryParam>()) { "Expected @QueryParam was null!" }
        val queryParamKey = param.value

        val parameterType = parameter.type
        parameterType

        check(ParamType.STRING.assertTypeMatch(parameter.type))

        val nullable = parameter.isNullable

        return ParameterResolver(parameter, nullable) resolve@ {
            return@resolve request.queryParam(queryParamKey)
        }
    }

    internal fun generate() {
        when(method) {
            HttpMethod.get -> get(path, handle)
            HttpMethod.post -> post(path, handle)

            else -> throw IllegalStateException("$method is not supported!")
        }
    }

    companion object {
        private fun KParameter.hasValidAnnotation(): Boolean {
            return hasAnnotation<Header>() || hasAnnotation<Param>() ||
                   hasAnnotation<QueryParam>() || hasAnnotation<Body>()
        }
    }

    private class ParameterResolver<T>(
        private val parameter: KParameter,
        private val nullable: Boolean,
        private val resolve: suspend RouteContext.() -> T
    ) {
        suspend operator fun invoke(context: RouteContext): Any? {
            val resolved = resolve(context)
            check(resolved !== null || nullable) {
                "When resolving ${parameter.name} got a null value " +
                "(index: ${parameter.index}, type: ${parameter.type})"
            }
            return resolved
        }
    }
}