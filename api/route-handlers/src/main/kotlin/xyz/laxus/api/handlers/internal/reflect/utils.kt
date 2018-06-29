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
@file:JvmName("Internal_Utils")
package xyz.laxus.api.handlers.internal.reflect

import java.lang.reflect.*
import kotlin.reflect.*
import kotlin.reflect.KVisibility.*
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.javaMethod

internal val KClass<*>.isAccessibleViaReflection: Boolean get() {
    return visibility?.isAccessibleViaReflection ?: java.isAccessibleViaReflection
}

internal val KCallable<*>.isAccessibleViaReflection: Boolean get() {
    return checkNotNull((this as? KProperty<*>)?.isAccessibleViaReflection ?:
                 (this as? KFunction<*>)?.isAccessibleViaReflection) {
        "Cannot calculate reflective accessibility of KCallable $this"
    }
}

internal val KProperty<*>.isAccessibleViaReflection: Boolean get() {
    return checkNotNull(visibility?.isAccessibleViaReflection ?:
                        javaGetter?.isAccessibleViaReflection ?:
                        javaField?.isAccessibleViaReflection) {
        "Cannot calculate reflective accessibility of KProperty $this"
    }
}

internal val KFunction<*>.isAccessibleViaReflection: Boolean get() {
    return checkNotNull(visibility?.isAccessibleViaReflection ?:
                        javaMethod?.isAccessibleViaReflection ?:
                        javaConstructor?.isAccessibleViaReflection) {
        "Cannot calculate reflective accessibility of KFunction $this"
    }
}

private val Class<*>.isAccessibleViaReflection get() = Modifier.isPublic(modifiers)
private val Method.isAccessibleViaReflection get() = Modifier.isPublic(modifiers)
private val Constructor<*>.isAccessibleViaReflection get() = Modifier.isPublic(modifiers)
private val Field.isAccessibleViaReflection get() = Modifier.isPublic(modifiers)
private val KVisibility.isAccessibleViaReflection get() = this != PRIVATE && this != PROTECTED

/**
 * Runs the [block] of code catching any [InvocationTargetException] thrown and
 * throwing the [target exception][InvocationTargetException.getTargetException]
 * instead.
 *
 * @param R The return type.
 *
 * @param block The block to run.
 *
 * @return The value returned from running the [block].
 *
 * @throws java.lang.Exception Any unwrapped [InvocationTargetException].
 */
@Throws(Exception::class)
internal inline fun <R> runInvocationSafe(block: () -> R): R {
    try {
        return block()
    } catch(e: InvocationTargetException) {
        throw e.targetException
    }
}