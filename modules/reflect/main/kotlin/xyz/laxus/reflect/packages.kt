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
@file:JvmName("PackagesUtil")
@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package xyz.laxus.reflect

import xyz.laxus.reflect.internal.impl.KPackageImpl
import java.net.URL
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass

/**
 * Wraps a reflected [Package] and exposes it's contents
 * with proper nullability.
 *
 * @author Kaidan Gustave
 * @since  0.1.0
 */
interface KPackage: KAnnotatedElement {
    val name: String
    val version: VersionInfo
    val title: TitleInfo
    val vendor: VendorInfo

    @Throws(NumberFormatException::class) fun isCompatibleWith(desired: String): Boolean

    fun isSealed(url: URL? = null): Boolean

    interface VersionInfo: InfoCategory
    interface TitleInfo: InfoCategory
    interface VendorInfo: InfoCategory

    interface InfoCategory {
        val implementation: String?
        val specification: String?
    }

    companion object {
        @get:JvmStatic val All: List<KPackage> get() {
            return Package.getPackages().map { KPackageImpl(it) }
        }
    }
}

fun packageOf(klazz: KClass<*>): KPackage = KPackageImpl(klazz)
fun packageOf(clazz: Class<*>): KPackage = KPackageImpl(clazz.kotlin)

val KClass<*>.packageInfo: KPackage get() = packageOf(this)
val Class<*>.packageInfo: KPackage get() = packageOf(this)

val Package.kotlin: KPackage get() = KPackageImpl(this)
val KPackage.java: Package get() {
    // TODO Replace with this in 1.3
    //require(this is KPackageImpl) { "Package '$this' is not a valid KPackage implementation" }
    //this.javaPackage
    val impl = requireNotNull(this as? KPackageImpl) {
        "Package '$this' is not a valid KPackage implementation"
    }
    return impl.javaPackage
}
