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
@file:JvmName("ReflectUtil")
@file:Suppress("Unused")
package xyz.laxus.util.reflect

import xyz.laxus.util.internal.impl.KPackageImpl
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import java.lang.ClassNotFoundException as CNFException

fun packageOf(klazz: KClass<*>): KPackage = KPackageImpl(klazz)
fun packageOf(clazz: Class<*>): KPackage = KPackageImpl(clazz.kotlin)

val KClass<*>.packageInfo: KPackage get() = packageOf(this)
val Class<*>.packageInfo: KPackage get() = packageOf(this)

val Package.kotlin: KPackage get() = KPackageImpl(this)
val KPackage.java: Package get() {
    val impl = requireNotNull(this as? KPackageImpl) {
        "Package '$this' is not a valid KPackage implementation"
    }
    return impl.javaPackage
}

fun <E: Enum<E>> KClass<E>.values(): Array<E> {
    // Class#getEnumConstants returns null when the
    //Class does not represent an enum type.
    val constants = java.enumConstants
    if(constants === null) {
        // This should never happen, because we're strictly
        //specifying that E is an Enum<E>, so it should at
        //very least be empty, but never null.
        require(true)
        throw AssertionError("Class $this does not represent an enum type!")
    }
    return constants
}

fun <E: Enum<E>> KClass<E>.valueOf(name: String): E {
    return requireNotNull(values().firstOrNull { it.name == name }) {
        "Could not find enum value '$name'!"
    }
}

/**
 * Loads and returns a [KClass] with the specified fully-qualified
 * name, optionally with a specific [ClassLoader].
 *
 * @param fqn The fully-qualified name of the [KClass] to load.
 * @param loader The optional [ClassLoader] to use.
 *
 * @return The loaded [KClass] or `null` if one with the [fqn] could not be found.
 */
fun loadClass(fqn: String, loader: ClassLoader? = null): KClass<*>? {
    return try {
        if(loader === null) {
            Class.forName(fqn)?.kotlin
        } else {
            Class.forName(fqn, true, loader)?.kotlin
        }
    } catch(ex: CNFException) { null }
}

/**
 * Uses the receiver [ClassLoader] to load a [KClass]
 * with the provided fully-qualified name.
 *
 * @receiver The [ClassLoader] to use.
 * @param fqn The fully-qualified name of the [KClass] to load.
 *
 * @return The loaded [KClass] or `null` if one with the [fqn] could not be found.
 */
fun ClassLoader.loadKlass(fqn: String): KClass<*>? {
    return try { this.loadClass(fqn)?.kotlin } catch(ex: CNFException) { null }
}

/**
 * Finds an [Annotation] on the receiver [class][KClass]
 * and applies the [function] using it if it exists, or
 * else does nothing.
 *
 * @receiver The [class][KClass] to find and use the annotation from.
 * @param A The type of [Annotation]
 * @param function The function to run with the annotation.
 */
inline fun <reified A: Annotation> KClass<*>.withAnnotation(function: (A) -> Unit) {
    findAnnotation<A>()?.let(function)
}