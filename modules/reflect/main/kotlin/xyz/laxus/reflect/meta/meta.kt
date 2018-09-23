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
@file:Suppress("DEPRECATED_JAVA_ANNOTATION")
package xyz.laxus.reflect.meta

import xyz.laxus.reflect.findAnnotations
import kotlin.reflect.KAnnotatedElement
import java.lang.annotation.Repeatable as JvmRepeatable

@MustBeDocumented
@JvmRepeatable(Meta::class)
@Retention(AnnotationRetention.RUNTIME)
annotation class MetaData(vararg val value: Meta)

@Repeatable
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
annotation class Meta(val key: String, val value: String)

fun KAnnotatedElement.meta(key: String): String? {
    return findAnnotations<Meta>().firstOrNull { it.key == key }?.value
}
