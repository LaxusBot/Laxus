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
package xyz.laxus.wyvern.internal.reflect

import java.lang.reflect.InvocationTargetException

/**
 * Runs the [block] of code catching any [InvocationTargetException] thrown and
 * throwing the [target exception][InvocationTargetException.getTargetException]
 * instead.
 *
 * @param R The return type.
 *
 * @param block The block to run.
 *
 * @return The value returned from running the [block].
 *
 * @throws java.lang.Exception Any unwrapped [InvocationTargetException].
 */
@Throws(Exception::class)
internal inline fun <R> runInvocationSafe(block: () -> R): R {
    try {
        return block()
    } catch(e: InvocationTargetException) {
        throw e.targetException
    }
}