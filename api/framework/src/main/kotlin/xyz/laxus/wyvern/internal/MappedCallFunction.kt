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
package xyz.laxus.wyvern.internal

import xyz.laxus.util.createLogger
import xyz.laxus.util.debug
import xyz.laxus.util.reflect.callSuspended
import xyz.laxus.util.reflect.hasAnnotation
import xyz.laxus.util.reflect.isNullable
import xyz.laxus.util.reflect.isType
import xyz.laxus.util.warn
import xyz.laxus.wyvern.API
import xyz.laxus.wyvern.annotation.*
import xyz.laxus.wyvern.http.CallContext
import xyz.laxus.wyvern.http.error.HttpError
import xyz.laxus.wyvern.http.error.badRequest
import xyz.laxus.wyvern.http.error.internalServerError
import xyz.laxus.wyvern.http.error.missingValue
import xyz.laxus.wyvern.http.header.ContentType
import xyz.laxus.wyvern.internal.annotation.responseHeaders
import xyz.laxus.wyvern.internal.params.ParamType
import xyz.laxus.wyvern.internal.params.ParameterResolver
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

/**
 * @author Kaidan Gustave
 */
internal open class MappedCallFunction protected constructor(
    protected val api: API,
    protected val base: Any,
    protected val function: KFunction<*>,
    headers: Map<String, String>
) {
    internal companion object {
        internal val Log = createLogger(MappedCallFunction::class)

        private fun CallContext.wrap(t: Throwable): HttpError {
            // During reflection we might encounter an InvocationTargetException
            //as a result of an http exception.
            // In this event we should return the wrapped exception as
            //long as it's an HttpError.
            val httpError = ((t as? InvocationTargetException)?.targetException as? HttpError)
                            ?: t as? HttpError
                            ?: internalServerError(t)
            response.status(httpError.status)
            return httpError
        }
    }

    protected var alreadyRegisteredBodyProvider = false
    protected var contentType = api.defaultContentType
        set(value) { field = if(value != ContentType.Any) value else api.defaultContentType }

    protected val responseHeaders = headers + function.responseHeaders
    protected val paramResolvers: Map<KParameter, ParameterResolver<*>>

    init {
        paramResolvers = hashMapOf()

        // map instance parameter
        function.instanceParameter?.let { instanceParam ->
            paramResolvers[instanceParam] = mapInstance(instanceParam)
        }

        // map extension parameter
        function.extensionReceiverParameter?.let { extParam ->
            paramResolvers[extParam] = mapExtension(extParam)
        }

        // map value parameters
        function.valueParameters.associateByTo(paramResolvers, { it }) {
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
                Log.warn { "Detected a parameter with default value for suspend function ${function.name}!" }
                Reporter.warnAboutNoDefaultOnSuspend()
            }
        }
        return null
    }

    private fun mapInstance(parameter: KParameter): ParameterResolver<Any> {
        logRegisteredResolver("Instance", parameter)
        return ParameterResolver(parameter, false, false) { base }
    }

    private fun mapExtension(parameter: KParameter): ParameterResolver<CallContext> {
        check(parameter.isType<CallContext>()) { "Extension receiver must have the type: ${CallContext::class}!" }
        check(!parameter.isNullable) { "Extension receiver may not be nullable!" }
        logRegisteredResolver("Extension", parameter)

        // Extension param cannot be null with framework
        return ParameterResolver(parameter, false, false) { this }
    }

    private fun mapBody(parameter: KParameter): ParameterResolver<*> {
        check(!alreadyRegisteredBodyProvider) { "Head parameter resolver has already been registered!" }
        val provider = checkNotNull(bodyProviders[parameter.type.jvmErasure]) {
            "Unable to convert parameter type ${parameter.type} without registered converter service!"
        }

        alreadyRegisteredBodyProvider = true
        contentType = provider.contentType

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
        val default = registerDefaultParameterValue(parameter)
        val defaultValue = headerType.convert(default)
        logRegisteredResolver("Header", parameter)
        return ParameterResolver(
            parameter = parameter,
            handleNull = { throw missingValue(headerKey, "header") },
            resolve = { headerType.convert(request.header(headerKey)) ?: defaultValue }
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
        val default = registerDefaultParameterValue(parameter)
        val defaultValue = queryParamType.convert(default)
        logRegisteredResolver("QueryParam", parameter)
        return ParameterResolver(
            parameter = parameter,
            handleNull = { throw missingValue(queryParamKey, "query param") },
            resolve = { queryParamType.convert(request.queryMap[queryParamKey]?.value()) ?: defaultValue }
        )
    }

    private fun logRegisteredResolver(type: String, parameter: KParameter) {
        Log.debug {
            "Registering $type resolver (${parameter.name ?: "receiver"}: ${parameter.type.jvmErasure.simpleName}) " +
            "for function ${base::class.simpleName} (${function.name})"
        }
    }

    // Create but do not dedicate storage to actual functional code bodies.

    protected val handleSuspended: suspend CallContext.() -> Any? get() = handle@ {
        try {
            prepare()
            check(function.isSuspend) { "Tried to invoke suspended handle for un-suspended function!" }
            val parameterValues = resolveUnmappedParameters()
            val value = function.callSuspended(*parameterValues.toTypedArray())
            handleReturns(api, this, value)
            return@handle value?.takeUnless { it === Unit } ?: ""
        } catch(t: Throwable) {
            val value = wrap(t).toJson() // FIXME There needs to be a conversion handler for different response types
            handleReturns(api, this, value)
            return@handle value
        }
    }

    protected val handle: CallContext.() -> Any? get() = handle@ {
        try {
            prepare()
            check(!function.isSuspend) { "Tried to invoke un-suspended handle for suspended function!" }
            val parameterValues = resolveMappedParams()
            val value = function.callBy(parameterValues)
            handleReturns(api, this, value)
            return@handle value?.takeUnless { it === Unit } ?: ""
        } catch(t: Throwable) {
            val value = wrap(t).toJson() // FIXME There needs to be a conversion handler for different response types
            handleReturns(api, this, value)
            return@handle value
        }
    }

    private fun CallContext.prepare() {
        response.header("Transfer-Encoding", null)
        responseHeaders.forEach { response.header(it.key, it.value) }
    }

    // Separate function used to work around default parameters of suspended functions
    // See: https://youtrack.jetbrains.net/issue/KT-21972
    //
    // This will be removed in a future release when possible.
    private fun CallContext.resolveUnmappedParameters(): List<Any?> {
        // map by order of parameters, map is not guaranteed to be consistently ordered
        return function.parameters.map { paramResolvers[it]?.invoke(this) }
    }

    private fun CallContext.resolveMappedParams(): Map<KParameter, Any?> {
        val map = linkedMapOf<KParameter, Any?>()
        for((parameter, resolver) in paramResolvers) {
            val value = resolver(this)
            if(value === null && parameter.isOptional) {
                continue
            }
            map[parameter] = value
        }
        return map
    }
}
