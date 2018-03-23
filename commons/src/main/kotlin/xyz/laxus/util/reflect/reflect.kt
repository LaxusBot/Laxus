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
package xyz.laxus.util.reflect

import xyz.laxus.util.internal.impl.KPackageImpl
import kotlin.reflect.KClass

// PackageInfo

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

fun loadClass(fqn: String): KClass<*>? {
    return try { Class.forName(fqn)?.kotlin } catch(ex: ClassNotFoundException) { null }
}