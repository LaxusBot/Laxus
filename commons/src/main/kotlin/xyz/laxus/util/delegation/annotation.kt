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
@file:Suppress("UNCHECKED_CAST")
package xyz.laxus.util.delegation

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType

class AnnotationDelegate<A: Annotation, out R>
@PublishedApi
internal constructor(
    private val type: KClass<A>,
    private val nullable: Boolean,
    private val checkProperty: Boolean,
    private val function: (A) -> R
) {
    @Volatile private var initialized = false
    @Volatile private var cached: R? = null
        get() {
            // Is this even initialized yet?
            check(initialized) {
                "Delegated value has not been initialized!"
            }

            // If it isn't nullable but is null, then somehow we initialized
            //the value incorrectly.
            // This should be impossible, but if it happens we shouldn't let
            //this throw an NPE.
            check(field !== null || !nullable) {
                "Unable to retrieve null value cached for not-null Annotation Delegate!"
            }
            return field
        }

    operator fun getValue(instance: Any, property: KProperty<*>): R {
        if(!initialized) {
            val annotations = instance::class.annotations
            val annotation = checkNotNull(annotations.firstOrNull { it::class.isSubclassOf(type) } ?: if(checkProperty) {
                property.annotations.firstOrNull { it::class.isSubclassOf(type) }
            } else null) {
                "Annotation $type was not found on ${instance::class}"
            }
            this.cached = function(annotation as A)
            this.initialized = true
        }
        return cached as R
    }
}

inline fun <reified A: Annotation, reified R>
    annotation(checkProperty: Boolean = true, noinline function: (A) -> R): AnnotationDelegate<A, R> {
    return AnnotationDelegate(A::class, R::class.starProjectedType.isMarkedNullable, checkProperty, function)
}