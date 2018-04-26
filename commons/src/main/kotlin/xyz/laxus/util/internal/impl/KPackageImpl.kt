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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.laxus.util.internal.impl

import xyz.laxus.annotation.Implementation
import xyz.laxus.util.hashAll
import xyz.laxus.util.reflect.KPackage
import java.net.URL
import kotlin.reflect.KClass

@PublishedApi
@Implementation
internal class KPackageImpl(
    internal val javaPackage: Package,
    override val name: String,
    override val version: KPackage.VersionInfo,
    override val title: KPackage.TitleInfo,
    override val vendor: KPackage.VendorInfo
): KPackage {
    override val annotations by lazy { javaPackage.annotations.mapNotNull { it } }

    constructor(klazz: KClass<*>): this(klazz.java.`package`)
    constructor(javaPackage: Package): this(
        javaPackage,
        javaPackage.name,
        VersionInfoImpl(javaPackage.implementationVersion, javaPackage.specificationVersion),
        TitleInfoImpl(javaPackage.implementationTitle, javaPackage.specificationTitle),
        VendorInfoImpl(javaPackage.implementationVendor, javaPackage.specificationVendor)
    )

    @Throws(NumberFormatException::class)
    override fun isCompatibleWith(desired: String): Boolean {
        return javaPackage.isCompatibleWith(desired)
    }

    override fun isSealed(url: URL?): Boolean {
        return url?.let { javaPackage.isSealed(it) } ?: javaPackage.isSealed
    }

    override fun toString(): String = javaPackage.toString()
    override fun hashCode(): Int = hashAll(javaPackage, name, version, title, vendor)
    override fun equals(other: Any?): Boolean {
        if(other !is KPackageImpl) {
            return false
        }

        return name  ==  other.name && version == other.version &&
               title == other.title && vendor  == other.vendor
    }

    private data class VersionInfoImpl(
        override val implementation: String?,
        override val specification: String?
    ): KPackage.VersionInfo
    private data class TitleInfoImpl(
        override val implementation: String?,
        override val specification: String?
    ): KPackage.TitleInfo
    private data class VendorInfoImpl(
        override val implementation: String?,
        override val specification: String?
    ): KPackage.VendorInfo
}