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
package xyz.laxus.wyvern.internal.params

import xyz.laxus.util.ignored
import xyz.laxus.util.modifyIf
import xyz.laxus.util.reflect.isCompatibleWith
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

/**
 * Used primarily for primitive parameter types found in
 * route and query params.
 *
 * @author Kaidan Gustave
 */
internal enum class ParamType: ParamConverter {
    STRING {
        override fun convert(given: Any?): String? {
            if(given === null) return null
            if(given is String) return given
            return given.toString()
        }
    },
    LONG {
        override fun convert(given: Any?): Long? {
            if(given is String) ignored { return given.toLong() }
            if(given is Int) return given.toLong()
            if(given is Long) return given
            return null
        }
    },
    INT {
        override fun convert(given: Any?): Int? {
            if(given is String) ignored { return given.toInt() }
            if(given is Int) return given
            return null
        }
    },
    BOOLEAN {
        override fun convert(given: Any?): Boolean? {
            if(given is String) {
                if(given.toLowerCase().let { it == "true" || it == "false" }) {
                    return given.toBoolean()
                }
            }
            if(given is Boolean) return given
            return null
        }
    },
    FLOAT {
        override fun convert(given: Any?): Float? {
            if(given is String) ignored { return given.toFloat() }
            if(given is Float) return given
            return null
        }
    },
    DOUBLE {
        override fun convert(given: Any?): Double? {
            if(given is String) ignored { return given.toDouble() }
            if(given is Float) return given.toDouble()
            if(given is Double) return given
            return null
        }
    };

    fun assertTypeMatch(type: KType): Boolean {
        val comparingType = when(this) {
            STRING -> String::class.starProjectedType
            LONG -> Long::class.starProjectedType
            INT -> Int::class.starProjectedType
            BOOLEAN -> Boolean::class.starProjectedType
            FLOAT -> Float::class.starProjectedType
            DOUBLE -> Double::class.starProjectedType
        }.modifyIf(type.isMarkedNullable) { it.withNullability(true) }

        return comparingType.isCompatibleWith(type)
    }

    companion object {
        @JvmStatic fun from(type: KType): ParamType? = values().find { it.assertTypeMatch(type) }
    }
}