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
@file:JvmName("TypesUtil")
@file:Suppress("Unused")
package xyz.laxus.reflect

import java.lang.reflect.Modifier
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.javaMethod

// KFunction

val KFunction<*>.isExtension get() = this.extensionReceiverParameter !== null

val KFunction<*>.isStatic get() = javaMethod?.let { Modifier.isStatic(it.modifiers) } ?: false

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
