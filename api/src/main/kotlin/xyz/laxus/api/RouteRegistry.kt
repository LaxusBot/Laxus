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
package xyz.laxus.api

import xyz.laxus.api.internal.RouteRunner
import xyz.laxus.api.annotation.Handle
import xyz.laxus.api.annotation.Route
import xyz.laxus.api.annotation.SubRoute
import xyz.laxus.api.annotation.responseHeaderAnnotations
import xyz.laxus.util.reflect.hasAnnotation
import kotlin.reflect.KClass
import kotlin.reflect.KVisibility
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

object RouteRegistry {
    fun register(any: Any) {
        val klazz = any::class
        val route = checkNotNull(klazz.findAnnotation<Route>()) {
            "$klazz is not annotated with @Route!"
        }
        registerWithRoute(any, klazz, route)
    }

    private fun registerWithRoute(
        any: Any, klazz: KClass<*>, route: Route,
        ancestors: List<Route> = emptyList(),
        headers: Map<String, String> = emptyMap()
    ) {
        val responseHeaders = headers + klazz.responseHeaderAnnotations.associate { it.header to it.value }

        val subHandlers = klazz.memberProperties.filter {
            it.hasAnnotation<SubRoute>() && it.visibility == KVisibility.PUBLIC
        }.mapNotNull { it.call(any) } + klazz.java.declaredClasses.mapNotNull {
            it.kotlin.objectInstance?.takeIf {
                it::class.hasAnnotation<Route>()
            }
        }

        subHandlers.forEach {
            val subKlazz = it::class
            val subRoute = checkNotNull(subKlazz.findAnnotation<Route>()) { "$subKlazz is not annotated with @Route!" }
            registerWithRoute(it, subKlazz, subRoute, ancestors + route, responseHeaders)
        }

        val prefix = ancestors.joinToString(separator = "") { it.path }
        klazz.functions.filter { it.hasAnnotation<Handle>() }.map {
            val handle = checkNotNull(it.findAnnotation<Handle>())
            RouteRunner(prefix + route.path + handle.extension, handle.method, any, it, responseHeaders)
        }.forEach { it.generate() }
    }
}