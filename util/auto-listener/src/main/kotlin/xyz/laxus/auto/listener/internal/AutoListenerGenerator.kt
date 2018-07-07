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
package xyz.laxus.auto.listener.internal

import com.squareup.kotlinpoet.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.SubscribeEvent
import xyz.laxus.auto.listener.AutoListener
import xyz.laxus.auto.listener.AutoListenerProcessor
import javax.annotation.Generated
import javax.lang.model.element.Element
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter.constructorsIn
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import kotlin.reflect.KClass

internal class AutoListenerGenerator(
    private val original: TypeElement,
    private val elements: Elements,
    private val types: Types
) {
    private val eventMap = mutableMapOf<KClass<out Event>, MutableList<Element>>()

    internal fun addEventElement(klass: KClass<out Event>, element: Element) {
        eventMap.computeIfAbsent(klass) { arrayListOf() } += element
    }

    internal fun build(name: String?): TypeSpec {
        val builder = TypeSpec.classBuilder(
            name ?: "${original.simpleName}${AutoListenerProcessor.GeneratedSuffix}"
        )

        original.annotationMirrors.asSequence().filter {
            !types.isSameType(
                it.annotationType.asElement().asType(),
                elements.getTypeElement(AutoListener::class.qualifiedName!!).asType()
            ) && !types.isSameType(
                it.annotationType.asElement().asType(),
                elements.getTypeElement("kotlin.Metadata").asType()
            )
        }.forEach { builder.addAnnotation(AnnotationSpec.get(it)) }

        builder.addAnnotation(
            AnnotationSpec.builder(Generated::class)
            .addMember("\"%L\"", "${AutoListenerProcessor::class.qualifiedName}")
            .build()
        )

        builder.addModifiers(KModifier.PRIVATE)
        builder.addSuperinterface(EventListener::class)
        builder.superclass(original.asClassName())

        original.interfaces.forEach { builder.addSuperinterface(it.asTypeName()) }
        original.constructors.forEach { constructor ->
            val spec = FunSpec.constructorBuilder().apply {
                val superCallCode = CodeBlock.builder()
                constructor.parameters.forEachIndexed { i, parameter ->
                    if(types.isSameType(parameter.asType(), elements.getTypeElement("java.lang.String").asType())) {
                        val spec = ParameterSpec.builder(parameter.simpleName.toString(), String::class).build()
                        addParameter(spec)
                    } else {
                        addParameter(ParameterSpec.get(parameter))
                    }

                    if(i > 0) superCallCode.add(", ")
                    superCallCode.add("${parameter.simpleName} = ${parameter.simpleName}")
                }
                callSuperConstructor(superCallCode.build())
            }.build()
            builder.addFunction(spec)
        }

        val typeVars = original.typeParameters
        if(typeVars.isNotEmpty()) {
            builder.addTypeVariables(typeVars.map { it.asTypeVariableName() })
            val superclass = with(ParameterizedTypeName) {
                original.asClassName().parameterizedBy(
                    *typeVars.map { it.asType().asTypeName() }.toTypedArray()
                )
            }
            builder.superclass(superclass)
        }

        val onEvent = FunSpec.builder("onEvent").apply {
            addModifiers(KModifier.OVERRIDE)
            addAnnotation(SubscribeEvent::class)
            addParameter(ParameterSpec.builder("event", Event::class).build())
            val onEventCode = CodeBlock.builder()
            onEventCode.beginControlFlow("when(event)")
            eventMap.forEach { type, functions ->
                onEventCode.beginControlFlow("is %T ->", type)
                functions.forEach { function ->
                    onEventCode.addStatement("%L(event)", function.simpleName.toString())
                }
                onEventCode.endControlFlow()
            }
            onEventCode.endControlFlow()
            addCode(onEventCode.build())
        }

        builder.addFunction(onEvent.build())

        return builder.build()
    }

    private companion object {
        val TypeElement.constructors: List<ExecutableElement> get() {
            val constructors = arrayListOf<ExecutableElement>()

            for(member in constructorsIn(enclosedElements)) {
                // Only get non-private constructors
                if(!member.modifiers.contains(Modifier.PRIVATE)) {
                    constructors += member
                }
            }

            return constructors
        }
    }
}
