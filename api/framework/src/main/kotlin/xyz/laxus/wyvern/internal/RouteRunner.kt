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

import spark.RouteImpl
import xyz.laxus.util.createLogger
import xyz.laxus.util.debug
import xyz.laxus.util.reflect.callBySuspended
import xyz.laxus.util.reflect.hasAnnotation
import xyz.laxus.util.reflect.isNullable
import xyz.laxus.util.reflect.isType
import xyz.laxus.wyvern.API
import xyz.laxus.wyvern.annotation.*
import xyz.laxus.wyvern.context.RouteContext
import xyz.laxus.wyvern.http.HttpMethod
import xyz.laxus.wyvern.http.error.HttpError
import xyz.laxus.wyvern.http.error.badRequest
import xyz.laxus.wyvern.http.error.internalServerError
import xyz.laxus.wyvern.http.error.missingValue
import xyz.laxus.wyvern.http.header.ContentType
import xyz.laxus.wyvern.internal.params.ParamType
import xyz.laxus.wyvern.internal.params.ParameterResolver
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.jvmErasure

internal class RouteRunner(
    private val path: String,
    private val method: HttpMethod,
    private val base: Any,
    private val function: KFunction<*>,
    headers: Map<String, String>
) {
    internal companion object {
        internal val Log = createLogger(RouteRunner::class)

        private fun RouteContext.wrap(t: Throwable): HttpError {
            val httpError = t as? HttpError ?:
                            (t as? InvocationTargetException)?.targetException as? HttpError
                            ?: internalServerError(t).also { Log.error("Internal Server Error", it) }
            response.status(httpError.status)
            return httpError
        }
    }

    private var alreadyRegisteredBodyProvider = false
    private var accept = API.DefaultContentType
        set(value) { field = if(value != ContentType.Any) value else API.DefaultContentType }

    private val responseHeaders = headers.toMutableMap()
    private val paramResolvers: List<ParameterResolver<*>>

    init {
        val paramResolvers = arrayListOf<ParameterResolver<*>>()
        val params = this.function.parameters.toMutableList()

        this.function.instanceParameter?.let { instanceParam ->
            // map instance parameter
            params -= instanceParam
            paramResolvers += mapInstance(instanceParam)
        }

        // Leaving this comment as a note in-case this changes:

        // Currently there is a very weird characteristic of
        //coroutine/suspended functions.
        // Apparently kotlin can tell when a function is suspended,
        //but cannot acquire the continuation parameter.
        // For those who don't know, the continuation parameter
        //is a hidden extra parameter with type "Continuation<R>"
        //where "R" is the return type of the function, located
        //at index "x + 1", where "x" is the number of visible
        //parameters:

        // This:
        //suspend fun example(str: String) {
        //    println(str)
        //}

        // Compiles to this:
        //public void example(String str, Continuation<Unit> continuation) {
        //    // Abstract coroutine code
        //}

        // The issue here is that while reflection does require this parameter
        //is provided, there's no real way to provide it as a resolved parameter
        //unless we're calling the function right then and there.

        this.function.extensionReceiverParameter?.let { extParam ->
            // map extension parameter
            params -= extParam
            paramResolvers += mapExtension(extParam)
        }

        params.mapTo(paramResolvers) {
            when {
                it.hasAnnotation<Body>()       -> mapBody(it)
                it.hasAnnotation<Header>()     -> mapHeader(it)
                it.hasAnnotation<Param>()      -> mapParam(it)
                it.hasAnnotation<QueryParam>() -> mapQueryParam(it)

                else -> throw IllegalStateException(
                    "Parameter ${it.name} is invalid (index: ${it.index}, type: ${it.type}, " +
                    "signature: ${base::class}#${function.name})"
                )
            }
        }

        this.paramResolvers = paramResolvers

        this.function.responseHeaderAnnotations.associateTo(this.responseHeaders) { it.header to it.value }
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

    private fun mapBody(parameter: KParameter): ParameterResolver<*> {
        check(!alreadyRegisteredBodyProvider) { "Head parameter resolver has already been registered!" }
        val provider = checkNotNull(RouteBodyHandler.bodyProviders[parameter.type.jvmErasure]) {
            "Unable to convert parameter type ${parameter.type} without registered converter service!"
        }

        alreadyRegisteredBodyProvider = true
        accept = provider.contentType

        logRegisteredResolver("Body", parameter)
        return ParameterResolver(
            parameter = parameter,
            handleNull = { throw badRequest("Request body is missing!") },
            resolve = { with(provider) { convert() } }
        )
    }

    private fun mapHeader(parameter: KParameter): ParameterResolver<*> {
        val header = checkNotNull(parameter.findAnnotation<Header>()) { "Expected @Header was null!" }
        val headerKey = checkNotNull(header.value.takeIf { it.isNotBlank() } ?: parameter.name) {
            "Could not generate header key from parameter: $parameter"
        }
        val headerType = checkNotNull(ParamType.from(parameter.type))
        logRegisteredResolver("Header", parameter)
        return ParameterResolver(
            parameter = parameter,
            handleNull = { throw missingValue(headerKey, "header") },
            resolve = { headerType.convert(request.header(headerKey)) }
        )
    }

    private fun mapParam(parameter: KParameter): ParameterResolver<*> {
        val param = checkNotNull(parameter.findAnnotation<Param>()) { "Expected @Param was null!" }
        val unformattedParamKey = checkNotNull(param.value.takeIf { it.isNotBlank() } ?: parameter.name) {
            "Could not generate param key from parameter: $parameter"
        }
        val paramKey = ":${unformattedParamKey.removePrefix(":")}"
        val paramType = checkNotNull(ParamType.from(parameter.type))
        logRegisteredResolver("Param", parameter)
        return ParameterResolver(
            parameter = parameter,
            handleNull = { throw missingValue(paramKey, "route param") },
            resolve = { paramType.convert(request.param(paramKey)) }
        )
    }

    private fun mapQueryParam(parameter: KParameter): ParameterResolver<*> {
        val queryParam = checkNotNull(parameter.findAnnotation<QueryParam>()) { "Expected @QueryParam was null!" }
        val queryParamKey = checkNotNull(queryParam.value.takeIf { it.isNotBlank() } ?: parameter.name) {
            "Could not generate query param key from parameter: $parameter"
        }
        val queryParamType = checkNotNull(ParamType.from(parameter.type))
        logRegisteredResolver("QueryParam", parameter)
        return ParameterResolver(
            parameter = parameter,
            handleNull = { throw missingValue(queryParamKey, "query param") },
            resolve = { queryParamType.convert(request.queryMap[queryParamKey]?.value()) }
        )
    }

    private fun logRegisteredResolver(type: String, parameter: KParameter) {
        Log.debug {
            "Registering $type resolver (${parameter.name ?: "receiver"}: ${parameter.type.jvmErasure.simpleName}) " +
            "for function ${base::class.simpleName} (${function.name})"
        }
    }

    private suspend fun RouteContext.handleSuspended() {
        try {
            prepare()
            check(function.isSuspend) { "Tried to invoke suspended handle for un-suspended function!" }
            val parameterValues = resolveParams()
            val value = function.callBySuspended(parameterValues)
            RouteBodyHandler.handleReturns(this, value)
            if(value !== Unit && value !== null) send(value) else finish()
        } catch(t: Throwable) {
            val value = wrap(t).toJson()
            RouteBodyHandler.handleReturns(this, value)
            send(value)
        }
    }

    private fun RouteContext.handle(): Any? {
        try {
            prepare()
            check(!function.isSuspend) { "Tried to invoke un-suspended handle for suspended function!" }
            val parameterValues = resolveParams()
            val value = function.callBy(parameterValues)
            RouteBodyHandler.handleReturns(this, value)
            return value?.takeUnless { it === Unit } ?: ""
        } catch(t: Throwable) {
            val value = wrap(t).toJson()
            RouteBodyHandler.handleReturns(this, value)
            return value
        }
    }

    private fun RouteContext.prepare() {
        response.header("Transfer-Encoding", null)
        responseHeaders.forEach { response.header(it.key, it.value) }
    }

    private fun RouteContext.resolveParams(): Map<KParameter, Any?> {
        return paramResolvers.mapNotNull {
            val value = it(this)
            if(value === null && it.optional)
                return@mapNotNull null
            return@mapNotNull it.parameter to value
        }.toMap(HashMap())
    }

    internal fun generate() {
        val route = if(function.isSuspend) {
            SuspendedRoute(path, { handleSuspended() })
        } else {
            NormalRoute(path, { handle() })
        }

        API.Spark.addRoute(method.toSparkHttpMethod(), RouteImpl.create(route.path, route))
    }
}