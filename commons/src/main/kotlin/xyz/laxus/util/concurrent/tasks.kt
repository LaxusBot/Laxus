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
package xyz.laxus.util.concurrent

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.future.asDeferred
import xyz.laxus.util.internal.CoroutineBody
import xyz.laxus.util.internal.impl.TaskImpl
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

/**
 * A combo-type that extends both [Deferred] and [CompletionStage].
 *
 * The implementation of this type can be constructed using either an existing
 * [CompletionStage] and the [asTask] extension, or using the top level [task]
 * function, which creates using a brand new [Deferred] instance.
 *
 * @since 0.5.0
 * @author Kaidan Gustave
 */
interface Task<T>: Deferred<T>, CompletionStage<T> {
    fun get(): T
    fun get(timeout: Long, unit: TimeUnit): T
}

/**
 * Creates a new [Task] using a [Deferred] created with the provided arguments
 * and the [async] function.
 */
fun <T> task(
    context: CoroutineContext = DefaultDispatcher,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    parent: Job? = null,
    block: CoroutineBody<T>
): Task<T> = TaskImpl(async(context = context, start =  start, parent = parent, block = block))

/**
 * Converts the receiver to a [Task].
 */
fun <T> CompletionStage<T>.asTask(): Task<T> {
    if(this is CompletableFuture<T>) {
        return TaskImpl(this)
    }
    return TaskImpl(asDeferred())
}