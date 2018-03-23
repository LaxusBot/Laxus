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
@file:Suppress("unused")

package xyz.laxus.api.routes

import io.ktor.application.log
import io.ktor.routing.*
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.full.*
import io.ktor.routing.Route as KtorRoute

typealias CallContext = io.ktor.pipeline.PipelineContext<Unit, io.ktor.application.ApplicationCall>

/**
 * @author Kaidan Gustave
 */
abstract class RouteController {
    private var _parent: RouteController? = null

    private val basePath: String? by lazy { this::class.findAnnotation<Path>()?.path }

    private val functions: List<RouteFunction> by lazy {
        this::class.memberExtensionFunctions.mapNotNull f@ { function ->
            val routeAnnotation = function.findAnnotation<Route>() ?: return@f null
            val path = buildString {
                _parent?.basePath?.let { append(it) }
                basePath?.let { append(it) }
                append(routeAnnotation.path)
            }
            RouteFunction(routeAnnotation.method, path) {
                suspendCoroutine<Unit> { cont ->
                    function.call(this@RouteController, this, cont)
                }
            }
        }
    }

    private val subControllers: List<RouteController> by lazy {
        this::class.nestedClasses.mapNotNull {
            it.takeIf { it.isSubclassOf(RouteController::class) }?.objectInstance as? RouteController
        }
    }

    val parent: RouteController? get() = _parent

    internal fun create(route: KtorRoute) {
        route.application.log.debug("Creating RouteController: ${this::class}")
        subControllers.forEach {
            it._parent = this
        }
        functions.forEach {
            route.application.log.debug("Applying $it")
            it.apply(route)
        }
    }

    class RouteFunction(
        private val method: Method,
        private val path: String,
        private val function: suspend CallContext.(Unit) -> Unit
    ) {
        fun apply(route: KtorRoute) = when(method) {
            Method.GET -> route.get(path, function)
            Method.PUT -> route.put(path, function)
            Method.POST -> route.post(path, function)
            Method.PATCH -> route.patch(path, function)
            Method.DELETE -> route.delete(path, function)
            Method.OPTIONS -> route.options(path, function)
            Method.HEAD -> route.head(path, function)
        }

        override fun toString(): String = "${method.name} - $path"
    }
}