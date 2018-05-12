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
package xyz.laxus.api.annotation

import xyz.laxus.util.collections.accumulate
import kotlin.annotation.AnnotationRetention.*
import kotlin.annotation.AnnotationTarget.*
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

/**
 * @author Kaidan Gustave
 */
@Retention(RUNTIME)
@Target(FUNCTION, CLASS, ANNOTATION_CLASS)
annotation class ResponseHeader(val header: String, val value: String)

// FIXME When Annotations can have members/inner types, reallocate these functions to ResponseHeader.Companion
internal val KAnnotatedElement.responseHeaderAnnotations: List<ResponseHeader> get() {
    return annotations.accumulate {
        it.responseHeaderAnnotation()
    }
}

internal fun Annotation.responseHeaderAnnotation(
    previouslyChecked: MutableSet<KClass<out Annotation>> = mutableSetOf()
): List<ResponseHeader> {
    if(this is ResponseHeader) return listOf(this)
    val klass = annotationClass
    if(klass in previouslyChecked) return emptyList()
    previouslyChecked += klass
    return klass.annotations.accumulate { it.responseHeaderAnnotation(previouslyChecked) }
}