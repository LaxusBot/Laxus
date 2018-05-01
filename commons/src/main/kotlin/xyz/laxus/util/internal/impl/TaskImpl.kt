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
package xyz.laxus.util.internal.impl

import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.future.asCompletableFuture
import kotlinx.coroutines.experimental.future.asDeferred
import xyz.laxus.annotation.Implementation
import xyz.laxus.util.concurrent.Task
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
@Implementation
internal class TaskImpl<T>
private constructor(deferred: Deferred<T>, private val completion: CompletableFuture<T>): Task<T>,
    Deferred<T> by deferred,
    CompletionStage<T> by completion {
    internal constructor(completion: CompletableFuture<T>): this(completion.asDeferred(), completion)
    internal constructor(deferred: Deferred<T>): this(deferred, deferred.asCompletableFuture())

    override fun get(): T {
        return completion.get()
    }

    override fun get(timeout: Long, unit: TimeUnit): T {
        return completion.get(timeout, unit)
    }
}