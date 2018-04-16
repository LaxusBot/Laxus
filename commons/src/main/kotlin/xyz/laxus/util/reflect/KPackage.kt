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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")
package xyz.laxus.util.reflect

import xyz.laxus.annotation.Experimental
import xyz.laxus.util.internal.impl.KPackageImpl
import java.net.URL
import kotlin.reflect.KAnnotatedElement

/**
 * Wraps a reflected [Package] and exposes it's contents
 * with proper nullability.
 *
 * @author Kaidan Gustave
 * @since  0.1.0
 */
@Experimental(details = """
    In the event that kotlin.reflect adds their own Package
    reflection entities, this may be subject to deprecation
    and/or removal.
""")
interface KPackage : KAnnotatedElement {
    companion object {
        @Deprecated(
            message = "Naming convention has been changed.",
            replaceWith = ReplaceWith(
                expression = "KPackage.All",
                imports = ["xyz.laxus.reflect.KPackage"]
            ),
            level = DeprecationLevel.HIDDEN
        )
        @get:JvmName("getAll_Deprecated")
        @get:JvmStatic val all: List<KPackage> get() = All
        @get:JvmStatic val All: List<KPackage> get() = Package.getPackages().map { KPackageImpl(it) }
    }

    val name: String
    val version: VersionInfo
    val title: TitleInfo
    val vendor: VendorInfo

    @Throws(NumberFormatException::class)
    fun isCompatibleWith(desired: String): Boolean

    fun isSealed(url: URL? = null): Boolean

    interface VersionInfo : InfoCategory
    interface TitleInfo : InfoCategory
    interface VendorInfo : InfoCategory

    interface InfoCategory {
        val implementation: String?
        val specification: String?
    }
}