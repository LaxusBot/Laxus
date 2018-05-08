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

import xyz.laxus.api.spark.annotation.Handle
import xyz.laxus.api.spark.annotation.Route
import xyz.laxus.util.reflect.hasAnnotation
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

/**
 * @author Kaidan Gustave
 */
object RouteRegistry {
    fun register(any: Any) {
        val klazz = any::class
        val info = checkNotNull(klazz.findAnnotation<Route>()) {
            "$klazz is not annotated with @Route!"
        }

        val handles = klazz.functions.filter { it.hasAnnotation<Handle>() }.map {
            val handle = checkNotNull(it.findAnnotation<Handle>())
            RouteRunner(info.path + handle.extension, handle.method, any, it)
        }

        handles.forEach { it.generate() }
    }
}