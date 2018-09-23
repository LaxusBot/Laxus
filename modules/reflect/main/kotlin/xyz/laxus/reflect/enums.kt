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
@file:JvmName("EnumsUtil")
package xyz.laxus.reflect

import kotlin.reflect.KClass

fun <E: Enum<E>> KClass<E>.values(): Array<E> {
    // Class#getEnumConstants returns null when the
    //Class does not represent an enum type.
    val constants = java.enumConstants
    if(constants === null) {
        // This should never happen, because we're strictly
        //specifying that E is an Enum<E>, so it should at
        //very least be empty, but never null.
        throw AssertionError("Class $this does not represent an enum type!")
    }
    return constants
}

fun <E: Enum<E>> KClass<E>.valueOf(name: String): E {
    return requireNotNull(values().firstOrNull { it.name == name }) {
        "Could not find enum value '$name'!"
    }
}
