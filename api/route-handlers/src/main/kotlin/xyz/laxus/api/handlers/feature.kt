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
@file:Suppress("UNUSED_PARAMETER", "unused")
@file:JvmName("RouteHandlersFeature")
package xyz.laxus.api.handlers

import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.routing.*
import io.ktor.util.AttributeKey
import xyz.laxus.api.handlers.annotations.*
import xyz.laxus.api.handlers.internal.getClassList
import xyz.laxus.api.handlers.internal.reflect.isAccessibleViaReflection
import xyz.laxus.api.handlers.internal.reflect.runInvocationSafe
import xyz.laxus.api.handlers.internal.routing.RouteFunction
import xyz.laxus.api.handlers.internal.routing.RouteHandlerInfo
import xyz.laxus.api.handlers.internal.routing.RouteLifecycle
import xyz.laxus.util.reflect.hasAnnotation
import java.lang.reflect.Modifier.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.*

class RouteHandlers private constructor(configuration: Configuration) {
    private val handlers = ArrayList<RouteHandlerInfo>(configuration.handlers.size)

    internal val handleMissingBody = configuration.handleMissing.body
    internal val handleMissingHeader = configuration.handleMissing.header
    internal val handleMissingParam = configuration.handleMissing.param
    internal val handleMissingQueryParam = configuration.handleMissing.queryParam

    init { configuration.handlers.forEach(this::registerWithRoute) }

    class Configuration internal constructor(application: Application) {
        internal val handlers = mutableListOf<Any>()
        internal val websocket = null as Any?
        internal val handleMissing = MissingConfigurations()

        init {
            application.environment.config.propertyOrNull("ktor.application.routes")
                ?.getClassList(application.environment.classLoader)
                ?.asSequence()
                ?.map { requireNotNull(it) { "Cannot load class!" } }
                ?.mapTo(handlers) { it.objectInstance ?: it.createInstance() }
        }

        @RouteHandlers.Dsl
        fun register(handler: Any) {
            this.handlers += handler
        }

        @RouteHandlers.Dsl
        fun handleMissing(block: RouteHandlers.MissingConfigurations.() -> Unit) {
            this.handleMissing.block()
        }
    }

    class MissingConfigurations internal constructor() {
        internal var body: suspend ApplicationCall.() -> Nothing = { genericError("body") }
        internal var header: suspend ApplicationCall.(String) -> Nothing = { genericError("header") }
        internal var param: suspend ApplicationCall.() -> Nothing = { genericError("param") }
        internal var queryParam: suspend ApplicationCall.() -> Nothing = { genericError("query param") }

        @RouteHandlers.Dsl
        fun body(block: suspend ApplicationCall.() -> Nothing) {
            this.body = block
        }

        @RouteHandlers.Dsl
        fun header(block: suspend ApplicationCall.(String) -> Nothing) {
            this.header = block
        }

        @RouteHandlers.Dsl
        fun param(block: suspend ApplicationCall.() -> Nothing) {
            this.param = block
        }

        @RouteHandlers.Dsl
        fun queryParam(block: suspend ApplicationCall.() -> Nothing) {
            this.queryParam = block
        }

        private companion object {
            @JvmStatic private fun genericError(identifier: String): Nothing {
                throw IllegalArgumentException("Missing $identifier")
            }
        }
    }

    private fun registerWithRoute(handler: Any) = registerWithRoute(handler, handler::class)

    private fun registerWithRoute(
        handler: Any,
        klazz: KClass<*>,
        route: RouteHandler = validateClass(klazz),
        ancestors: List<RouteHandler> = emptyList()
    ) {
        require(route !in ancestors) { "Detected recursive route resolution!" }

        val prefix = ancestors.joinToString("/", prefix = "/") { it.path.removePrefix("/") }
        val eligible = (klazz.memberFunctions + klazz.memberExtensionFunctions).filterInvalid()
        val authenticated = klazz.findAnnotation<Authenticated>()
        val handles = eligible.entries.mapTo(mutableListOf()) { entry ->
            val methodAnnotation = checkNotNull(entry.key.annotationClass.findAnnotation() ?: entry.key as? Method)
            val method = HttpMethod.parse(methodAnnotation.value)
            val annotationClass = entry.key.annotationClass

            val pathExtension = annotationClass.findAnnotation<PathExtension>()?.let {
                val annotationMethods = annotationClass.java.methods.filter {
                    !isStatic(it.modifiers) && it.returnType == String::class.java
                }
                annotationMethods.find {
                    return@find it.isAnnotationPresent(PathExtension::class.java)
                } ?: annotationMethods.firstOrNull()
            }

            val path = pathExtension?.invoke(entry.key) as String? ?: ""
            return@mapTo RouteFunction(this, handler, method, entry.value, path, authenticated)
        }

        val lifecycle = RouteLifecycle(handler, klazz)
        handlers += RouteHandlerInfo(prefix + route.path, lifecycle, handles)

        findSubRoutes(klazz, handler).forEach {
            val subKlazz = it::class
            val subRoute = checkNotNull(subKlazz.findAnnotation<RouteHandler>()) {
                "$subKlazz is not annotated with @Route!"
            }
            registerWithRoute(
                handler = it,
                klazz = subKlazz,
                route = subRoute,
                ancestors = ancestors + route
            )
        }
    }

    companion object Feature: ApplicationFeature<Application, Configuration, RouteHandlers> {
        override val key = AttributeKey<RouteHandlers>("RouteHandlers")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit): RouteHandlers {
            val routeHandlers = RouteHandlers(Configuration(pipeline).apply(configure))

            pipeline.routing {
                routeHandlers.handlers.forEach { info ->
                    info.handles.forEach { handle ->
                        val authenticated = handle.authenticated
                        if(authenticated === null) {
                            createRoute(info, handle)
                        } else {
                            requireNotNull(pipeline.featureOrNull(Authentication)) { "Authentication is not installed!" }
                            authenticate(authenticated.name) { createRoute(info, handle) }
                        }
                    }
                }
            }

            // monitor for lifecycle events

            pipeline.environment.monitor.subscribe(ApplicationStarting) { application ->
                routeHandlers.handlers.forEach { handler ->
                    handler.lifecycle.initializers.forEach { it.run(application) }
                }
            }

            pipeline.environment.monitor.subscribe(ApplicationStopping) { application ->
                routeHandlers.handlers.forEach { handler ->
                    handler.lifecycle.destroyers.forEach { it.run(application) }
                }
            }

            return routeHandlers
        }

        @JvmStatic private fun Route.createRoute(info: RouteHandlerInfo, handle: RouteFunction) {
            when(handle.method) {
                HttpMethod.Get -> get(info.path + handle.path, handle.func)
                HttpMethod.Put -> put(info.path + handle.path, handle.func)
                HttpMethod.Post -> post(info.path + handle.path, handle.func)
                HttpMethod.Patch -> patch(info.path + handle.path, handle.func)
                HttpMethod.Delete -> delete(info.path + handle.path, handle.func)
                HttpMethod.Options -> options(info.path + handle.path, handle.func)
                HttpMethod.Head -> head(info.path + handle.path, handle.func)
                else -> throw IllegalArgumentException("Cannot accept route: ${handle.method} - ${info.path}")
            }
        }

        @JvmStatic private fun findSubRoutes(klazz: KClass<*>, handler: Any): List<Any> {
            require(klazz.isInstance(handler)) { "$handler is not an instance of $klazz" }

            val subRoutes = mutableListOf<Any>()

            // member properties
            for(prop in klazz.memberProperties) {
                if(prop.isAccessibleViaReflection && prop.hasAnnotation<SubRoute>()) {
                    subRoutes += runInvocationSafe { prop.call(handler) } ?: continue
                }
            }

            // nested classes
            for(subKlass in klazz.nestedClasses) {
                if(subKlass.hasAnnotation<SubRoute>() || subKlass.hasAnnotation<RouteHandler>()) {
                    require(subKlass.isAccessibleViaReflection) { "Cannot access annotated nested class $subKlass!" }
                    subRoutes += when {
                        subKlass.isInner -> {
                            val constructor = requireNotNull(subKlass.constructors.find {
                                it.isAccessibleViaReflection && it.valueParameters.isEmpty()
                            }) {
                                "Cannot instantiate nested class $subKlass because " +
                                "it has no valid constructors!"
                            }
                            runInvocationSafe { constructor.call(handler) }
                        }
                        else -> subKlass.objectInstance ?: subKlass.createInstance()
                    }
                }
            }

            return subRoutes
        }

        @JvmStatic private fun validateClass(klass: KClass<*>, sub: RouteHandler? = null): RouteHandler {
            require(!klass.isAbstract) { "Cannot process abstract class type: $klass" }
            require(!klass.isInner) { "Cannot process inner class type: $klass" }
            return sub ?: requireNotNull(klass.findAnnotation()) {
                "Could not find @RouteHandler on class: $klass"
            }
        }

        @JvmStatic private fun Collection<KFunction<*>>.filterInvalid(): Map<Annotation, KFunction<*>> {
            return this.asSequence().filter { f ->
                !f.isInline && f.typeParameters.isEmpty() &&
                !f.isExternal && f.isAccessibleViaReflection
            }.mapNotNull { f ->
                f.annotations.firstOrNull {
                    it.annotationClass.hasAnnotation<Method>() || it is Method
                }?.let { it to f }
            }.toMap()
        }
    }

    @DslMarker internal annotation class Dsl
}

@RouteHandlers.Dsl fun Application.routeHandlers(
    configure: RouteHandlers.Configuration.() -> Unit
) = install(RouteHandlers, configure)
