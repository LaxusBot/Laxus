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
package xyz.laxus.api.handlers.internal.routing

import xyz.laxus.api.handlers.annotations.Destroy
import xyz.laxus.api.handlers.annotations.Initialize
import xyz.laxus.api.handlers.internal.LifeCycleFunction
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberExtensionFunctions
import kotlin.reflect.full.memberFunctions

internal data class PathLifeCycle(private val handler: Any, private val klass: KClass<*>) {
    internal val initializers: Set<LifeCycleFunction>
    internal val destroyers: Set<LifeCycleFunction>

    init {
        val searchPool = klass.memberFunctions + klass.memberExtensionFunctions
        this.initializers = searchPool.mapViaAnnotation(Initialize::class)
        this.destroyers = searchPool.mapViaAnnotation(Destroy::class)
    }

    private fun <A: Annotation>
        Collection<KFunction<*>>.mapViaAnnotation(type: KClass<A>): Set<LifeCycleFunction> {
        return this.asSequence()
            .filter { it.annotations.any { it.annotationClass.isSubclassOf(type) } }
            .mapTo(hashSetOf()) { LifeCycleFunction(handler, it) }
    }
}