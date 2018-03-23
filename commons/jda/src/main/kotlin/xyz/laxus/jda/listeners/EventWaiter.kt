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
package xyz.laxus.jda.listeners

import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener
import org.slf4j.Logger
import xyz.laxus.util.collections.concurrentSet
import xyz.laxus.util.createLogger
import xyz.laxus.util.ignored
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.superclasses

/**
 * @author Kaidan Gustave
 */
class EventWaiter
private constructor(dispatcher: CoroutineDispatcher): EventListener, CoroutineContext by dispatcher {
    private companion object LOG: Logger by createLogger(EventWaiter::class)
    private val tasks = ConcurrentHashMap<KClass<*>, MutableSet<ITask<*>>>()

    constructor(): this(newFixedThreadPoolContext(3, "EventWaiter"))

    inline fun <reified E: Event> waitFor(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline timeout: (suspend () -> Unit)? = null,
        noinline condition: suspend (E) -> Boolean,
        noinline action: suspend (E) -> Unit
    ) = waitForEvent(E::class, condition, action, delay, unit, timeout)

    inline fun <reified E: Event> receive(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline condition: suspend (E) -> Boolean
    ): Deferred<E?> = receiveEvent(E::class, condition, delay, unit)

    fun <E: Event> waitForEvent(
        klazz: KClass<E>,
        condition: suspend (E) -> Boolean,
        action: suspend (E) -> Unit,
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        timeout: (suspend () -> Unit)? = null
    ) {
        val eventList = taskListType(klazz)
        val waiting = QueuedTask(condition, action)

        eventList += waiting

        if(delay > 0) {
            launch(this) {
                delay(delay, unit)
                if(eventList.remove(waiting))
                    timeout?.invoke()
            }
        }
    }

    fun <E: Event> receiveEvent(
        klazz: KClass<E>,
        condition: suspend (E) -> Boolean,
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS
    ): Deferred<E?> {
        val deferred = CompletableDeferred<E?>()
        val eventList = taskListType(klazz)
        val waiting = AwaitableTask(condition, deferred)

        eventList += waiting

        if(delay > 0) {
            launch(this) {
                delay(delay, unit)
                eventList.remove(waiting)
                // The receiveEvent method is supposed to return null
                // if no matching Events are fired within its
                // lifecycle.
                // Regardless of whether or not the AwaitableTask
                // was removed, we invoke this. In the event that
                // it has not completed, we need to make sure the
                // coroutine does not deadlock.
                deferred.complete(null)
            }
        }

        return deferred
    }

    override fun onEvent(event: Event) {
        launch(this) {
            val klazz = event::class
            dispatchEventType(event, klazz)
            klazz.superclasses.forEach { dispatchEventType(event, it) }
        }
    }

    private fun <E: Event> taskListType(klazz: KClass<E>): MutableSet<ITask<E>> {
        @Suppress("UNCHECKED_CAST")
        return tasks[klazz].let {
            it as? MutableSet<ITask<E>> ?: concurrentSet<ITask<E>>().also {
                tasks[klazz] = it as MutableSet<ITask<*>>
            }
        }
    }

    private suspend fun <T: Event> dispatchEventType(event: T, klazz: KClass<*>) {
        val set = tasks[klazz] ?: return
        @Suppress("RemoveExplicitTypeArguments", "UNCHECKED_CAST")
        set -= set.filterTo(hashSetOf<ITask<*>>()) {
            val waiting = (it as ITask<T>)
            waiting(event)
        }
    }

    private interface ITask<in T: Event> {
        suspend operator fun invoke(event: T): Boolean
    }

    private class QueuedTask<in T: Event>(
        private val condition: suspend (T) -> Boolean,
        private val action: suspend (T) -> Unit
    ): ITask<T> {
        override suspend operator fun invoke(event: T): Boolean {
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

    private class AwaitableTask<in T: Event>(
        private val condition: suspend (T) -> Boolean,
        private val completion: CompletableDeferred<T?>
    ): ITask<T> {
        override suspend operator fun invoke(event: T): Boolean {
            try {
                if(condition(event)) {
                    completion.complete(event)
                    return true
                }
                return false
            } catch(t: Throwable) {
                // In the case this ever throws an error,
                // we need to complete this exceptionally.
                completion.completeExceptionally(t)
                return true
            }
        }
    }
}
