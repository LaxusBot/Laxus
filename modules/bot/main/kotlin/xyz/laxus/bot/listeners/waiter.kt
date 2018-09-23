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
@file:Suppress("unused")
package xyz.laxus.bot.listeners

import kotlinx.coroutines.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.laxus.commons.collections.concurrentHashMap
import xyz.laxus.commons.collections.concurrentSet
import xyz.laxus.utils.createLogger
import xyz.laxus.utils.ignored
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.cast
import kotlin.reflect.full.isSubclassOf

private typealias WaiterCancellation = suspend () -> Unit
private typealias WaiterCondition<E> = suspend (E) -> Boolean
private typealias WaiterAction<E>    = suspend (E) -> Unit

class EventWaiter(
    private val dispatcher: ThreadPoolDispatcher = newFixedThreadPoolContext(3, "EventWaiter")
): EventListener, AutoCloseable by dispatcher, CoroutineContext by dispatcher {
    private companion object {
        private val Log = createLogger(EventWaiter::class)
    }

    private val tasks = concurrentHashMap<KClass<*>, MutableSet<Task<*>>>()

    inline fun <reified E: Event> waitFor(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline timeout: WaiterCancellation? = null,
        noinline condition: WaiterCondition<E>,
        noinline action: WaiterAction<E>
    ) = waitForEvent(E::class, condition, action, delay, unit, timeout)

    inline fun <reified E: Event> receive(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline condition: WaiterCondition<E>
    ): Deferred<E?> = receiveEvent(E::class, condition, delay, unit)

    suspend inline fun <reified E: Event> delayUntil(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline condition: WaiterCondition<E>
    ): Boolean = delayUntilEvent(E::class, condition, delay, unit)

    fun <E: Event> waitForEvent(
        klazz: KClass<E>,
        condition: WaiterCondition<E>,
        action: WaiterAction<E>,
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        timeout: WaiterCancellation? = null
    ) {
        registerTask(delay, unit, klazz, Task.QueuedTask(condition, action, timeout))
    }

    fun <E: Event> receiveEvent(
        klazz: KClass<E>,
        condition: WaiterCondition<E>,
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS
    ): Deferred<E?> {
        val waiting = Task.AwaitableTask(condition)
        registerTask(delay, unit, klazz, waiting)
        return waiting.completion
    }

    suspend fun <E: Event> delayUntilEvent(
        klazz: KClass<E>,
        condition: WaiterCondition<E>,
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS
    ): Boolean = receiveEvent(klazz, condition, delay, unit).await() !== null
    private fun <E: Event>
        registerTask(delay: Long, unit: TimeUnit, klazz: KClass<E>, waiting: Task<E>) {
        val eventSet = taskSetType(klazz)

        Log.debug("Adding task type: '$klazz'")
        eventSet += waiting

        if(delay > 0) {
            GlobalScope.launch(dispatcher) {
                delay(delay, unit)
                if(eventSet.remove(waiting)) {
                    Log.debug("Removing task type: '$klazz'")
                    if(waiting is Task.QueuedTask) {
                        waiting.timeout?.invoke()
                    }
                }

                if(waiting is Task.AwaitableTask) {
                    // The receiveEvent method is supposed to return null
                    //if no matching Events are fired within its
                    //lifecycle.
                    // Regardless of whether or not the AwaitableTask
                    //was removed, we invoke this. In the event that
                    //it has not completed, we need to make sure the
                    //coroutine does not deadlock.
                    waiting.completion.complete(null)
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E: Event> taskSetType(klazz: KClass<E>): MutableSet<Task<E>> {
        return tasks.computeIfAbsent(klazz) { concurrentSet() } as MutableSet<Task<E>>
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <E: Event> dispatchEventType(event: E, klazz: KClass<out E>) {
        val set = tasks[klazz] ?: return
        val filtered = set.filterTo(hashSetOf()) { (it as Task<E>).run(event) }
        Log.debug("Removing ${filtered.size} tasks with type: '$klazz'")
        set -= filtered
        if(set.isEmpty()) {
            tasks -= klazz
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onEvent(event: Event) {
        GlobalScope.launch(dispatcher) {
            val klazz = event::class
            dispatchEventType(event, klazz)
            klazz.allSuperclasses.asSequence()
                .filter  { it.isSubclassOf(Event::class) }
                .map     { it as KClass<out Event> }
                .forEach { dispatchEventType(it.cast(event), it) }
        }

        if(event is ShutdownEvent) close()
    }

    override fun close() = dispatcher.close()

    private sealed class Task<in E: Event> {
        abstract suspend fun run(event: E): Boolean

        class QueuedTask<in E: Event>(
            private val condition: WaiterCondition<E>,
            private val action: WaiterAction<E>,
            internal val timeout: WaiterCancellation?
        ): Task<E>() {
            override suspend fun run(event: E): Boolean {
                // Ignore exception, return false
                ignored {
                    if(condition(event)) {
                        // Ignore exception, return true
                        ignored { action(event) }
                        return true
                    }
                }
                return false
            }
        }

        class AwaitableTask<E: Event>(private val condition: WaiterCondition<E>): Task<E>() {
            internal val completion: CompletableDeferred<E?> = CompletableDeferred()

            override suspend fun run(event: E): Boolean {
                try {
                    if(condition(event)) {
                        completion.complete(event)
                        return true
                    }
                    return false
                } catch(t: Throwable) {
                    // In the case this ever throws an error,
                    //we need to complete this exceptionally.
                    completion.completeExceptionally(t)
                    return true
                }
            }
        }
    }
}
