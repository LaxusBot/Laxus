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
package xyz.laxus.api.spark.annotation

import xyz.laxus.util.modifyIf
import xyz.laxus.util.reflect.isCompatibleWith
import kotlin.reflect.KType
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.full.withNullability

/**
 * @author Kaidan Gustave
 */
enum class ParamType {
    STRING, LONG, INT;

    fun assertTypeMatch(type: KType): Boolean {
        val comparingType = when(this) {
            STRING -> String::class.starProjectedType
            LONG -> Long::class.starProjectedType
            INT -> Int::class.starProjectedType
        }.modifyIf({ type.isMarkedNullable }) { it.withNullability(true) }

        return comparingType.isCompatibleWith(type)
    }
}