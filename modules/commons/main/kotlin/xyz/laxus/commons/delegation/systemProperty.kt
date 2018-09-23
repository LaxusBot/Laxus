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
package xyz.laxus.commons.delegation

import xyz.laxus.utils.doNotSupport
import xyz.laxus.utils.propertyOf

class SystemPropertyDelegate internal constructor(
    private val key: String,
    private val storeFirst: Boolean,
    private val default: String?
): AbstractVarDelegate<String>() {
    private lateinit var storedValue: String

    override fun get(): String {
        if(storeFirst && ::storedValue.isInitialized) {
            return storedValue
        }

        val property = requireNotNull(propertyOf(key) ?: default) { "System property for '$key' was not found!" }

        if(storeFirst) {
            storedValue = property
        }

        return property
    }

    override fun set(value: String) {
        doNotSupport(storeFirst) { "Stored value overwriting is not supported!" }

        System.setProperty(key, value)
    }
}

fun systemProperty(
    key: String,
    storeFirst: Boolean = false,
    default: String? = null
) = SystemPropertyDelegate(key, storeFirst, default)
