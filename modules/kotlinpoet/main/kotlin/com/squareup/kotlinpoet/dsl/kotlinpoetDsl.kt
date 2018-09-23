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
package com.squareup.kotlinpoet.dsl

import com.squareup.kotlinpoet.*
import javax.lang.model.element.ExecutableElement
import kotlin.reflect.KClass
import kotlin.reflect.KType

@DslMarker
@Retention(AnnotationRetention.SOURCE)
annotation class KotlinPoetDsl

private typealias BuilderFunction<T> = T.() -> Unit

@KotlinPoetDsl
inline fun fileSpec(
    packageName: String,
    fileName: String,
    builder: BuilderFunction<FileSpec.Builder>
): FileSpec = FileSpec.builder(packageName, fileName).apply(builder).build()

@KotlinPoetDsl
inline fun FileSpec.Builder.funSpec(
    name: String,
    builder: BuilderFunction<FunSpec.Builder>
) {
    addFunction(FunSpec.builder(name).apply(builder).build())
}

@KotlinPoetDsl
inline fun FileSpec.Builder.classSpec(
    name: String,
    builder: BuilderFunction<TypeSpec.Builder>
) {
    addType(TypeSpec.classBuilder(name).apply(builder).build())
}

@KotlinPoetDsl
inline fun TypeSpec.Builder.annotationSpec(
    type: KClass<out Annotation>,
    builder: BuilderFunction<AnnotationSpec.Builder>
) {
    addAnnotation(AnnotationSpec.builder(type).apply(builder).build())
}

@KotlinPoetDsl
inline fun TypeSpec.Builder.propertySpec(
    name: String,
    type: KClass<*>,
    vararg modifier: KModifier,
    builder: BuilderFunction<PropertySpec.Builder>
) {
    addProperty(PropertySpec.builder(name, type, *modifier).apply(builder).build())
}

@KotlinPoetDsl
inline fun TypeSpec.Builder.propertySpec(
    name: String,
    type: KType,
    vararg modifier: KModifier,
    builder: BuilderFunction<PropertySpec.Builder>
) {
    addProperty(PropertySpec.builder(name, type.asTypeName(), *modifier).apply(builder).build())
}

@KotlinPoetDsl
inline fun TypeSpec.Builder.funSpec(name: String, builder: BuilderFunction<FunSpec.Builder>) {
    addFunction(FunSpec.builder(name).apply(builder).build())
}

@KotlinPoetDsl
inline fun TypeSpec.Builder.overriding(executable: ExecutableElement, builder: BuilderFunction<FunSpec.Builder>) {
    addFunction(FunSpec.overriding(executable).apply(builder).build())
}

@KotlinPoetDsl
inline fun PropertySpec.Builder.initializer(builder: BuilderFunction<CodeBlock.Builder>) {
    initializer(CodeBlock.builder().apply(builder).build())
}

