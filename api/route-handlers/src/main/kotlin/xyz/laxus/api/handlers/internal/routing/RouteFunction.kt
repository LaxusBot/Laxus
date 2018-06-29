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
package xyz.laxus.api.handlers.internal.routing

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.Headers
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.pipeline.PipelineContext
import io.ktor.request.ApplicationRequest
import io.ktor.request.receiveOrNull
import io.ktor.response.ApplicationResponse
import io.ktor.response.respond
import xyz.laxus.api.handlers.RouteHandlers
import xyz.laxus.api.handlers.annotations.*
import xyz.laxus.api.handlers.internal.Reporter
import xyz.laxus.api.handlers.internal.reflect.ParamMapped
import xyz.laxus.api.handlers.internal.reflect.ParamType
import xyz.laxus.api.handlers.internal.routing.params.*
import xyz.laxus.api.handlers.internal.reflect.runInvocationSafe
import xyz.laxus.util.createLogger
import xyz.laxus.util.debug
import xyz.laxus.util.reflect.callSuspended
import xyz.laxus.util.reflect.hasAnnotation
import xyz.laxus.util.reflect.isNullable
import xyz.laxus.util.reflect.isType
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.*
import kotlin.reflect.jvm.jvmErasure

internal class RouteFunction(
    val feature: RouteHandlers,
    val instance: Any,
    val method: HttpMethod,
    val function: KFunction<*>,
    val path: String,
    authenticated: Authenticated?
) {
    private companion object {
        private val Log = createLogger(RouteFunction::class)
        private val ListType = List::class.createType(listOf(KTypeProjection.STAR), false)
    }

    val authenticated: Authenticated? = authenticated ?: function.findAnnotation()

    val code: HttpStatusCode? = function.findAnnotation<Code>()?.let { HttpStatusCode.fromValue(it.value) }

    private var alreadyRegisteredBodyProvider = false

    private val resolvers: Map<KParameter, RouteParamResolver<*>> = function.parameters.associateBy({ it }) { parameter ->
        when(parameter) {
            function.instanceParameter -> mapInstance(parameter)
            function.extensionReceiverParameter -> mapExtension(parameter)
            else -> when(parameter.type.jvmErasure) {
                ApplicationCall::class -> RouteCallResolver
                ApplicationRequest::class -> RouteRequestResolver
                ApplicationResponse::class -> RouteResponseResolver
                Headers::class -> RouteHeadersResolver
                Parameters::class -> when {
                    parameter.hasAnnotation<QueryParam>() -> RouteQueryParametersResolver
                    parameter.hasAnnotation<Param>() -> RouteParametersResolver
                    else -> throw IllegalArgumentException(
                        "Intended resolution is unspecified! A argument of type ${Parameters::class} " +
                        "should be annotated with either @QueryParam or @Param explicitly!"
                    )
                }
                else -> {
                    val annotation = parameter.annotations.find { it.annotationClass.hasAnnotation<ParamMapped>() }
                    when(annotation) {
                        is QueryParam -> mapQueryParam(annotation, parameter)
                        is Param -> mapParam(annotation, parameter)
                        is Header -> mapHeader(annotation, parameter)
                        is Body -> mapBody(parameter)
                        is Locate -> mapLocation(parameter)
                        else -> throw IllegalArgumentException("$parameter is not valid to be mapped!")
                    }
                }
            }
        }
    }

    private fun registerDefaultParameterValue(parameter: KParameter): String? {
        val default = parameter.findAnnotation<Default>()
        if(default !== null) {
            // If the function is not a suspend function we need to
            //warn the user that this behavior is not supported yet.
            if(!function.isSuspend) {
                Reporter.warnAboutDefaultOnNonSuspend()
            }

            Log.debug("Registering parameter default value of ${parameter.name} as ${default.value}!")
            return default.value
        } else {
            // Similar to the comments above, we should inform the user
            //that a default on a suspend function will not work as intended,
            //and to replace it with.
            if(function.isSuspend && parameter.isOptional) {
                // Always warn about a default value for a suspend function
                Log.warn("Detected a parameter with default value for suspend function ${function.name}!")
                Reporter.warnAboutNoDefaultOnSuspend()
            }
        }
        return null
    }

    private fun mapInstance(parameter: KParameter): RouteParamResolver<Any> {
        logRegisteredResolver("Instance", parameter)
        return RouteInstanceResolver(instance)
    }

    private fun mapExtension(parameter: KParameter): RouteParamResolver<ApplicationCall> {
        require(!parameter.isNullable) { "Extension receiver may not be nullable!" }
        require(parameter.isType<ApplicationCall>()) { "Extension receiver must have the type: ${ApplicationCall::class}!" }
        logRegisteredResolver("Extension", parameter)
        return RouteCallResolver
    }

    private fun mapBody(parameter: KParameter): RouteParamResolver<*> {
        require(!alreadyRegisteredBodyProvider) { "Body parameter resolver has already been registered!" }
        val erasure = parameter.type.jvmErasure
        alreadyRegisteredBodyProvider = true
        logRegisteredResolver("Body", parameter)
        return RouteFunctionalResolver(
            parameter = parameter,
            handleNull = feature.handleMissingBody,
            function = { receiveOrNull(erasure) }
        )
    }

    private fun mapHeader(header: Header, parameter: KParameter): RouteParamResolver<*> {
        val headerKey = header.value.takeIf { it.isNotBlank() } ?: parameter.name!!
        val isList = parameter.type.isSubtypeOf(ListType)
        val headerType = if(isList) {
            val type = checkNotNull(parameter.type.arguments.getOrNull(0)?.type) {
                "Could not resolve list type parameter for $parameter"
            }
            ParamType.from(type)
        } else ParamType.from(parameter.type)

        requireNotNull(headerType) { "Cannot resolve type for header: $headerKey!" }

        val default = registerDefaultParameterValue(parameter)
        val defaultValue = headerType!!.convert(default)
        val function: suspend ApplicationCall.() -> Any? = if(isList) {
            { request.headers.getAll(headerKey)?.map { headerType.convert(it) } ?: listOf(defaultValue) }
        } else {
            { headerType.convert(request.headers[headerKey]) ?: defaultValue }
        }

        logRegisteredResolver("Header", parameter)
        return RouteFunctionalResolver(
            parameter = parameter,
            handleNull = { feature.handleMissingHeader(this, headerKey) },
            function = function
        )
    }

    private fun mapParam(param: Param, parameter: KParameter): RouteParamResolver<*> {
        val paramType = requireNotNull(ParamType.from(parameter.type)) { "${parameter.type} is not a valid @Param type!" }
        val paramKey = param.value.takeIf { it.isNotBlank() } ?: parameter.name!!
        logRegisteredResolver("Param", parameter)
        return RouteFunctionalResolver(
            parameter = parameter,
            handleNull = feature.handleMissingParam,
            function = { paramType.convert(parameters[paramKey]) }
        )
    }

    private fun mapQueryParam(queryParam: QueryParam, parameter: KParameter): RouteParamResolver<*> {
        val queryParamKey = queryParam.value.takeIf { it.isNotBlank() } ?: parameter.name!!
        val isList = parameter.type.isSubtypeOf(ListType)
        val queryParamType = if(isList) {
            val type = checkNotNull(parameter.type.arguments.getOrNull(0)?.type) {
                "Could not resolve list type parameter for $parameter"
            }
            ParamType.from(type)
        } else ParamType.from(parameter.type)
        val default = registerDefaultParameterValue(parameter)
        val defaultValue = queryParamType!!.convert(default)
        val function: suspend ApplicationCall.() -> Any? = if(isList) {
            { request.queryParameters.getAll(queryParamKey)?.map { queryParamType.convert(it) } ?: listOf(defaultValue) }
        } else {
            { queryParamType.convert(request.queryParameters[queryParamKey]) ?: defaultValue }
        }

        logRegisteredResolver("QueryParam", parameter)
        return RouteFunctionalResolver(
            parameter = parameter,
            handleNull = feature.handleMissingQueryParam,
            function = function
        )
    }

    private fun mapLocation(parameter: KParameter): RouteParamResolver<*> {
        val erasure = parameter.type.jvmErasure
        require(erasure.hasAnnotation<io.ktor.locations.Location>()) {
            "Cannot map non-location parameter annotated with @Locate: $erasure"
        }
        return RouteLocationResolver(erasure)
    }

    private fun logRegisteredResolver(type: String, parameter: KParameter) {
        Log.debug {
            "Registering $type resolver (${parameter.name ?: "receiver"}: ${parameter.type.jvmErasure.simpleName}) " +
            "for function ${instance::class.simpleName} (${function.name})"
        }
    }

    val func: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit get() = {
        val value = runInvocationSafe {
            if(function.isSuspend) {
                function.callSuspended(*call.resolveUnmappedParameters().toTypedArray())
            } else {
                function.callBy(call.resolveMappedParams())
            }
        }
        call.handleAfter()
        if(value !== Unit && value !== null) call.respond(value)
    }

    private fun ApplicationCall.handleAfter() {
        code?.let { response.status(code) }
    }

    // Unfortunately, this is a workaround default parameters of suspended functions
    // See: https://youtrack.jetbrains.net/issue/KT-21972
    //
    // This will be removed in a future release when possible.
    private suspend fun ApplicationCall.resolveUnmappedParameters(): List<Any?> {
        // map by order of parameters, map is not guaranteed to be consistently ordered
        return function.parameters.map { resolvers[it]?.resolve(this) }
    }

    private suspend fun ApplicationCall.resolveMappedParams(): Map<KParameter, Any?> {
        val map = linkedMapOf<KParameter, Any?>()
        for((parameter, resolver) in resolvers) {
            val value = resolver.resolve(this)
            if(value === null && parameter.isOptional) continue
            map[parameter] = value
        }
        return map
    }
}
