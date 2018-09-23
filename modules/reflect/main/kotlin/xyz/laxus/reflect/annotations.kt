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
@file:JvmName("AnnotationsUtil")
@file:Suppress("Unused")
package xyz.laxus.reflect

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.KTypeProjection
import kotlin.reflect.KVariance
import kotlin.reflect.full.cast
import kotlin.reflect.full.createType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.starProjectedType
import java.lang.annotation.Repeatable as JvmRepeatable

val KClass<out Annotation>.retention get() = findAnnotation<Retention>()?.value ?: RUNTIME
val KClass<out Annotation>.targets get() = findAnnotation<Target>()?.allowedTargets ?: emptyArray()

inline fun <reified A: Annotation> KAnnotatedElement.hasAnnotation() = findAnnotation<A>() !== null

inline fun <reified A: Annotation> KAnnotatedElement.findAnnotations(): List<A> =
    findAnnotationsOf(A::class)

@PublishedApi
internal fun <A: Annotation> KAnnotatedElement.findAnnotationsOf(type: KClass<A>): List<A> {
    val container = annotations.find {
        it.annotationClass.findAnnotation<JvmRepeatable>()?.value == type
    } ?: return backupFinder(type)

    val value = container.annotationClass.java.methods.find { it.name == "value" } ?:
                return backupFinder(type)

    Array<Any>::class.createType(listOf(KTypeProjection(KVariance.OUT, type.starProjectedType)))
    @Suppress("UNCHECKED_CAST")
    return (value.invoke(container) as Array<A>).toList()
}

private fun <A: Annotation> KAnnotatedElement.backupFinder(type: KClass<A>): List<A> {
    return annotations.find { type.isInstance(it) }
               ?.let { listOf(type.cast(it)) } ?: emptyList()
}
