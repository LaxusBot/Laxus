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
package xyz.laxus.auto.listener

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import net.dv8tion.jda.core.events.Event
import xyz.laxus.auto.listener.internal.AutoListenerGenerator
import xyz.laxus.util.processor.ProcessorFrame
import java.nio.file.Paths
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedOptions
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic.Kind.*
import kotlin.reflect.full.isSubclassOf

@SupportedAnnotationTypes("xyz.laxus.auto.listener.AutoListener")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(AutoListenerProcessor.KaptKotlinGenerated)
internal class AutoListenerProcessor: ProcessorFrame() {
    override val supported = setOf(AutoListener::class)
    override val version = SourceVersion.RELEASE_8

    private val specs = mutableSetOf<TypeSpec>()

    override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val valid = annotations.filter { isAutoListener(it) }
        try {
            valid.forEach { annotation ->
                roundEnv.getElementsAnnotatedWith(annotation).forEach e@ { element ->
                    if(element.kind !== ElementKind.CLASS) return@e
                    processElement(element as TypeElement)
                }
            }
        } catch(t: Throwable) {
            messager.printMessage(ERROR, "An unexpected error occurred while processing")
            messager.printMessage(ERROR, "$t\n${t.stackTrace.joinToString("\n", prefix = "  ")}")
            return true
        }

        // All annotations were valid and claimed as a result
        return annotations.size == valid.size
    }

    private fun processElement(element: TypeElement) {
        val generator = AutoListenerGenerator(element, elements, types)
        val autoListener = element.getAnnotation(AutoListener::class.java)

        for(e in elements.getAllMembers(element).mapNotNull { it as? ExecutableElement }) {
            if(Modifier.STATIC in e.modifiers || Modifier.PRIVATE in e.modifiers) continue

            val params = e.parameters
            if(params.size > 1 || params.isEmpty()) continue

            val param = params[0]
            val paramType = types.asElement(param.asType()) ?: continue
            val packageName = elements.getPackageOf(paramType).qualifiedName.toString()
            val className = "$packageName.${paramType.simpleName}"
            try {
                val klass = Class.forName(className)
                if(klass.kotlin.isSubclassOf(Event::class)) {
                    generator.addEventElement(klass.asSubclass(Event::class.java).kotlin, e)
                }
            } catch(ex: ClassNotFoundException) {
                messager.printMessage(ERROR, "Could not find Event class for '$className'!")
            }
        }

        val spec = try {
            generator.build(autoListener.value.takeIf { it.isNotEmpty() })
        } catch(t: Throwable) {
            messager.printMessage(ERROR, "An error occurred while processing ${element.simpleName}", element)
            messager.printMessage(ERROR, "$t\n${t.stackTrace.joinToString("\n", prefix = "  ")}")
            return
        }

        val name = spec.name ?: "${element.simpleName}$GeneratedSuffix"
        val file = FileSpec.builder("${elements.getPackageOf(element).qualifiedName}", name).apply {
            addComment("Generated Auto-Listener!\n")
            addComment("This file should not be modified!\n")
            addComment("Modifications will be removed upon recompilation!")
            addType(spec)
            addFunction(FunSpec.builder("create${element.simpleName}").apply {
                returns(element.asClassName())
                addStatement("return %N()", spec)
            }.build())
        }.build()

        val genDir = requireNotNull(processingEnv.options[KaptKotlinGenerated]) {
            "Could not find processing environment option $KaptKotlinGenerated"
        }

        val genTarget = Paths.get(genDir).toFile()

        messager.printMessage(NOTE, "Writing file to ${genTarget.path}")

        try {
            file.writeTo(genTarget)
        } catch(t: Throwable) {
            messager.printMessage(ERROR, "An error occurred while writing to $genTarget", element)
            messager.printMessage(ERROR, "$t\n${t.stackTrace.joinToString("\n", prefix = "  ")}")
            return
        }

        specs += spec
    }

    internal companion object {
        const val GeneratedSuffix = "__Generated"
        const val KaptKotlinGenerated = "kapt.kotlin.generated"

        private fun isAutoListener(element: TypeElement): Boolean {
            return element.qualifiedName.toString() == AutoListener::class.java.canonicalName
                   || element.getAnnotation(AutoListener::class.java) !== null
        }
    }
}