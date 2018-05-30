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
@file:JvmName("ResponseHeadersUtil")
package xyz.laxus.wyvern.internal.annotation

import xyz.laxus.util.collections.accumulate
import xyz.laxus.wyvern.annotation.ResponseHeader
import xyz.laxus.wyvern.annotation.ResponseHeaders
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

internal val KAnnotatedElement.responseHeaders: Map<String, String> get() {
    return headerAnnotations.associateBy(ResponseHeader::header, ResponseHeader::value)
}

private val KAnnotatedElement.headerAnnotations get() = annotations.accumulate { it.responseHeaderAnnotation() }

private fun Annotation.responseHeaderAnnotation(
    previouslyChecked: MutableSet<KClass<out Annotation>> = mutableSetOf()
): List<ResponseHeader> {
    if(this is ResponseHeader) return listOf(this)
    if(this is ResponseHeaders) return this.headers.toList()
    val klass = annotationClass
    if(klass in previouslyChecked) return emptyList()
    previouslyChecked += klass
    return klass.annotations.accumulate { it.responseHeaderAnnotation(previouslyChecked) }
}