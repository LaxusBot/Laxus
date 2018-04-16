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
@file:Suppress("ObjectPropertyName", "MemberVisibilityCanBePrivate")
package xyz.laxus.util.delegation

import xyz.laxus.util.collections.ConcurrentFixedSizeCache
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.starProjectedType

class AnnotationDelegate<A: Annotation, out R>
@PublishedApi
internal constructor(
    private val type: KClass<A>,
    private val nullable: Boolean,
    private val checkClass: Boolean,
    private val checkProperty: Boolean,
    private val function: (A) -> R
) {
    /**
     * Configurations for [annotation delegates][AnnotationDelegate].
     */
    companion object Cache {
        /**
         * Whether or not [annotation delegates][AnnotationDelegate] will
         * globally cache per-class annotations.
         */
        var CacheEnabled = true
            set(value) {
                // on -> off
                if(!value && field) {
                    actualClassCache = null
                    actualPropertyCache = null
                }
                // off -> on
                if(value && !field) {
                    rebootCaches()
                }
                field = value
            }

        /**
         * The size of this cache.
         *
         * Annotations are mapped and cached in a first-in-first-out (FIFO)
         * system under two separate maps. One map exists for caching class-level
         * annotations in bulk, and the other exists for caching single
         * delegated annotations of individual properties.
         *
         * By setting the cache's size, the size of both these caches will
         * be set.
         *
         * Note: upon setting this, previously cached entries will be cleared.
         */
        var Size = 50
            set(value) {
                require(CacheEnabled) { "Cannot set size while cache is disabled!" }
                require(value > 0) { "Size must be greater than 0!" }
                field = value
                rebootCaches(value)
            }

        // Because of the very nature of AnnotationDelegates being that
        //they are bound specifically to one type of annotation, we only
        //globally cache the single delegated value per-property.
        private var actualClassCache: MutableMap<KClass<*>, List<Annotation>>? = null
        private var actualPropertyCache: MutableMap<KProperty<*>, Annotation>? = null

        // Note: both caches are internal for testing purposes

        internal val GlobalClassCache get() = checkNotNull(actualClassCache) {
            "When getting global class annotation cache, actual backing property was null!"
        }

        internal val GlobalPropertyCache get() = checkNotNull(actualPropertyCache) {
            "When getting global property annotation cache, actual backing property was null!"
        }

        init {
            rebootCaches()
        }

        private fun rebootCaches(size: Int = Size) {
            actualClassCache = ConcurrentFixedSizeCache(size)
            actualPropertyCache = ConcurrentFixedSizeCache(size)
        }
    }

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
            var annotation: Annotation? = null

            // first pass: check property
            if(checkProperty) {
                // check property cache if enabled
                if(CacheEnabled) {
                    annotation = GlobalPropertyCache[property]
                }

                if(annotation === null) {
                    // As a means to save overhead, we don't scan for annotations
                    //on the property unless we're supposed to check the property.
                    annotation = property.annotations.find { it::class.isSubclassOf(type) }
                }
            }

            // second pass: check class
            if(annotation === null && checkClass) {
                val klass = instance::class
                val allAnnotations = if(CacheEnabled) {
                    // cache the class's annotations.
                    GlobalClassCache.computeIfAbsent(klass, KClass<*>::annotations)
                } else klass.annotations

                annotation = allAnnotations.find { it::class.isSubclassOf(type) }
            }

            // verification pass
            annotation = checkNotNull(annotation) { "Annotation $type was not found" }

            if(CacheEnabled) {
                if(checkProperty) {
                    GlobalPropertyCache[property] = annotation
                }
            }

            @Suppress("UNCHECKED_CAST")
            this.cached = function(annotation as A)
            this.initialized = true
        }
        @Suppress("UNCHECKED_CAST")
        return cached as R
    }
}

inline fun <reified A: Annotation, reified R> annotation(
    checkClass: Boolean = true,
    checkProperty: Boolean = false,
    noinline function: (A) -> R
): AnnotationDelegate<A, R> {
    val nullable = R::class.starProjectedType.isMarkedNullable
    return AnnotationDelegate(A::class, nullable, checkClass, checkProperty, function)
}