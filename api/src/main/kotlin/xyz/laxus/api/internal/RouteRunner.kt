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
import xyz.laxus.api.annotation.*
import xyz.laxus.api.internal.context.RouteContext
import xyz.laxus.api.util.ContentType
import xyz.laxus.util.createLogger
import xyz.laxus.util.debug
import xyz.laxus.util.ignored
import xyz.laxus.util.modifyIf
import xyz.laxus.util.reflect.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.instanceParameter

internal class RouteRunner(
    private val path: String,
    private val method: HttpMethod,
    private val base: Any,
    private val function: KFunction<*>,
    headers: Map<String, String>
) {
    private companion object {
        private val Log = createLogger(RouteRunner::class)
        private fun KParameter.hasValidAnnotation(): Boolean {
            return hasAnnotation<Header>()     || hasAnnotation<Param>() ||
                   hasAnnotation<QueryParam>() || hasAnnotation<Body>()
        }
    }

    private val responseHeaders = headers.toMutableMap()
    private val paramResolvers: List<ParameterResolver<*>>
    private val handle: RouteHandle = {
        responseHeaders.forEach { response.header(it.key, it.value) }
        val parameterValues = resolveParams().toMap()
        val value = if(function.isSuspend) {
            function.callBySuspended(parameterValues)
        } else {
            function.callBy(parameterValues)
        }

        if(value !== Unit && value !== null) send(value) else finish()
    }

    init {
        val paramResolvers = arrayListOf<ParameterResolver<*>>()
        val params = function.parameters.toMutableList()

        function.instanceParameter?.let { instanceParam ->
            // map instance parameter
            params -= instanceParam
            paramResolvers += mapInstance(instanceParam)
        }

        function.extensionReceiverParameter?.let { extParam ->
            // map extension parameter
            params -= extParam
            paramResolvers += mapExtension(extParam)
        }

        params.forEach {
            check(it.hasValidAnnotation()) {
                "Parameter ${it.name} is invalid (index: ${it.index}, type: ${it.type}, " +
                "signature: ${base::class}#${function.name})"
            }

            paramResolvers += when {
                it.hasAnnotation<Header>()     -> mapHeader(it)
                it.hasAnnotation<Param>()      -> mapParam(it)
                it.hasAnnotation<QueryParam>() -> mapQueryParam(it)

                else -> throw IllegalStateException(
                    "Unable to map param '$it' for function " +
                    "'$function' in handler '${base::class}'"
                )
            }
        }

        this.paramResolvers = paramResolvers

        function.responseHeaderAnnotations.associateTo(this.responseHeaders) { it.header to it.value }
    }

    private suspend fun RouteContext.resolveParams(): Array<Pair<KParameter, Any?>> {
        return paramResolvers.mapNotNull {
            val value = it(this)
            if(value === null && it.optional)
                return@mapNotNull null
            return@mapNotNull it.parameter to value
        }.toTypedArray()
    }

    private fun mapInstance(parameter: KParameter): ParameterResolver<Any> {
        logRegisteredResolver("Instance", parameter)
        return ParameterResolver(parameter, false, false) { base }
    }

    private fun mapExtension(parameter: KParameter): ParameterResolver<RouteContext> {
        check(parameter.isType<RouteContext>()) { "Extension receiver must have the type: ${RouteContext::class}!" }
        check(!parameter.isNullable) { "Extension receiver may not be nullable!" }
        logRegisteredResolver("Extension", parameter)
        // Extension param cannot be null with framework
        return ParameterResolver(parameter, false, false) { this }
    }

    private fun mapHeader(parameter: KParameter): ParameterResolver<*> {
        val header = checkNotNull(parameter.findAnnotation<Header>()) { "Expected @Header was null!" }
        val headerKey = header.value
        val isContentType = headerKey.equals("Content-Type", true)
        val nullable = parameter.isType<String?>() || (isContentType && parameter.isType<ContentType?>())
        logRegisteredResolver("Header", parameter)
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
        val paramType = checkNotNull(ParamType.from(parameter.type))
        logRegisteredResolver("Param", parameter)
        return ParameterResolver(parameter) resolve@ {
            val paramValue = request.params[paramKey]
            if(paramValue === null) {
                return@resolve null
            }

            when(paramType) {
                ParamType.STRING -> return@resolve paramValue
                ParamType.INT -> return@resolve ignored(null) { paramValue.toInt() }
                ParamType.LONG -> return@resolve ignored(null) { paramValue.toLong() }
                ParamType.BOOLEAN -> return@resolve paramValue.toBoolean()
            }
        }
    }

    private fun mapQueryParam(parameter: KParameter): ParameterResolver<*> {
        val param = checkNotNull(parameter.findAnnotation<QueryParam>()) { "Expected @QueryParam was null!" }
        val queryParamKey = param.value
        val queryParamType = checkNotNull(ParamType.from(parameter.type))
        logRegisteredResolver("QueryParam", parameter)
        return ParameterResolver(parameter) resolve@ {
            val queryMap = request.queryMap[queryParamKey]
            if(queryMap.value() === null) {
                return@resolve null
            }

            when(queryParamType) {
                ParamType.STRING -> return@resolve queryMap.value()
                ParamType.INT -> return@resolve ignored(null) { queryMap.integerValue() }
                ParamType.LONG -> return@resolve ignored(null) { queryMap.longValue() }
                ParamType.BOOLEAN -> return@resolve queryMap.booleanValue()
            }
        }
    }

    private fun logRegisteredResolver(type: String, parameter: KParameter) {
        Log.debug {
            "Registering $type resolver ${parameter.name}: ${parameter.type} " +
            "for function ${base::class}#${function.name}"
        }
    }

    internal fun generate() {
        when(method) {
            HttpMethod.get -> get(path, handle)
            HttpMethod.post -> post(path, handle)
            HttpMethod.delete -> delete(path, handle)

            else -> throw IllegalStateException("$method is not supported!")
        }
    }
}