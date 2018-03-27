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
package xyz.laxus.api.util.concurrent

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.async
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.experimental.CoroutineContext

class Task<T> internal constructor(
    context: CoroutineContext,
    block: suspend CoroutineScope.() -> T
): CompletableFuture<T>() {
    init {
        val def = async(context) {
            val r = block()
            complete(r)
            return@async r
        }
        def.invokeOnCompletion {
            it?.let { completeExceptionally(it) }
        }
        def.start()
    }
}

fun <T> task(context: CoroutineContext = DefaultDispatcher,
             block: suspend CoroutineScope.() -> T): Task<T> {
    return Task(context, block)
}