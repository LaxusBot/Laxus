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
package xyz.laxus.jda

import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.IEventManager
import xyz.laxus.jda.listeners.SuspendedListener
import xyz.laxus.util.createLogger
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class ContextEventManager : IEventManager {
    companion object {
        private val LOG = createLogger(ContextEventManager::class)
        // For debugging
        /*@JvmField*/ val ThreadNumber = AtomicInteger(0)
    }

    private val context = newCachedThreadPool(Factory()).asCoroutineDispatcher()
    private val listeners = HashSet<Any>()

    override fun handle(event: Event) {
        val job = launch(context, CoroutineStart.LAZY) {
            listeners.forEach { listener ->
                try {
                    (listener as? EventListener)?.onEvent(event)
                    (listener as? SuspendedListener)?.onEvent(event)
                } catch(t: Throwable) {
                    LOG.warn("A listener encountered an exception:", t)
                }
            }
        }

        job.invokeOnCompletion {
            it?.let { LOG.error("A job encountered an exception:", it) }
        }

        job.start()
    }

    override fun register(listener: Any) {
        listeners += requireNotNull(listener as? SuspendedListener ?: listener as? EventListener) {
            "Listener must implement EventListener or SuspendedListener!"
        }
        LOG.debug("Registered listener to manager")
    }

    override fun getRegisteredListeners(): MutableList<Any> = listeners.toMutableList()

    override fun unregister(listener: Any) {
        listeners -= listener as? SuspendedListener ?: listener as? EventListener ?: return
        LOG.debug("Unregistered listener to manager")
    }

    private inner class Factory : ThreadFactory {
        private val name get() = "ContextEventManager Thread - ${ThreadNumber.getAndIncrement()}"
        override fun newThread(r: Runnable) = Thread(r, name).also { it.isDaemon = true }
    }
}
