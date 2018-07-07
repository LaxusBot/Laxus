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
@file:Suppress("unused")
package xyz.laxus.auto.listener.internal

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass

internal abstract class ProcessorFrame: AbstractProcessor() {
    private lateinit var _types: Types
    private lateinit var _elements: Elements
    private lateinit var _filer: Filer
    private lateinit var _messager: Messager

    protected abstract val supported: Set<KClass<out Annotation>>
    protected abstract val version: SourceVersion
    protected open val options: Set<String> get() = emptySet()

    protected val types get() = _types
    protected val elements get() = _elements
    protected val filer get() = _filer
    protected val messager get() = _messager

    override fun getSupportedSourceVersion() = version
    override fun getSupportedOptions() = options.toMutableSet()
    override fun getSupportedAnnotationTypes() = supported.mapNotNull { it.qualifiedName }.toMutableSet()

    @Synchronized override fun init(processingEnv: ProcessingEnvironment) {
        this._types = processingEnv.typeUtils
        this._elements = processingEnv.elementUtils
        this._filer = processingEnv.filer
        this._messager = processingEnv.messager
        this.processingEnv = processingEnv
    }
}