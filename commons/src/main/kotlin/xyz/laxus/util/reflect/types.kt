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
@file:Suppress("Unused")
package xyz.laxus.util.reflect

import java.lang.reflect.Modifier
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.javaMethod

// KFunction

val KFunction<*>.isExtension get() = this.extensionReceiverParameter !== null

val KFunction<*>.isStatic get() = javaMethod?.let { Modifier.isStatic(it.modifiers) } ?: false

val KFunction<*>.suspendParameter: KParameter get() {
    require(isSuspend) { "Function is not suspendable!" }
    return valueParameters.last()
}

suspend fun KFunction<*>.callSuspended(vararg args: Any?): Any? {
    require(isSuspend) { "Function is not suspendable!" }
    return suspendCoroutine { cont ->
        try {
            cont.resume(call(*args, cont))
        } catch(t: Throwable) {
            cont.resumeWithException(t)
        }
    }
}

suspend fun KFunction<*>.callBySuspended(args: Map<KParameter, Any?>): Any? {
    require(isSuspend) { "Function is not suspendable!" }
    return suspendCoroutine { cont ->
        try {
            val allArgs = if(args is MutableMap<KParameter, Any?>) {
                // Call to add the last parameter instead of creating a new map
                args.also { it[this.suspendParameter] = cont }
            } else args + (this.suspendParameter to cont)

            cont.resume(callBy(allArgs))
        } catch(t: Throwable) {
            cont.resumeWithException(t)
        }
    }
}

// KParameter

val KParameter.isNullable get() = type.isMarkedNullable

inline fun <reified T> KParameter.isType(): Boolean {
    val argKType = T::class.starProjectedType
    val recKType = this.type
    return recKType.isCompatibleWith(argKType)
}

// KTypeProjection

inline val KTypeProjection.isStarProjection get() = this.variance === null && this.type === null


// KType

fun KType.isCompatibleWith(other: KType): Boolean {
    // Nullability varies.
    // Note this only matters if the receiver is not marked nullable,
    //since we can provide a not null KType as a nullable KType, but
    //not visa-versa.
    if(other.isMarkedNullable && !isMarkedNullable) return false
    return other.isSupertypeOf(this)
}