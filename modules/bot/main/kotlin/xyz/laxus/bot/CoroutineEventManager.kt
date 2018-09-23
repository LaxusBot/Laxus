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
package xyz.laxus.bot

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.IEventManager
import xyz.laxus.bot.listeners.SuspendListener
import xyz.laxus.utils.createLogger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newFixedThreadPool
import kotlin.coroutines.suspendCoroutine

internal class CoroutineEventManager(size: Int = 5): IEventManager {
    private val threadGroup = ThreadGroup("JDAEventManager")

    @Volatile private var isShutdown = false
    private val threadNumber = atomic(0)

    private val listeners = hashSetOf<Any>()
    private val context = newFixedThreadPool(size) pool@ { r ->
        val name = "${threadGroup.name} - Thread ${threadNumber.getAndIncrement()}"
        return@pool Thread(threadGroup, r, name).also { it.isDaemon = true }
    }.asCoroutineDispatcher()

    override fun handle(event: Event?) {
        checkNotNull(event) { "JDA provided an event that was null!" }
        if(isShutdown) {
            return Log.warn(
                "Received an event after executor was shut down:\n" +
                "Type: ${event::class}\n" +
                "Response Number: ${event.responseNumber}"
            )
        }

        for(listener in listeners) {
            GlobalScope.launch(context) {
                when(listener) {
                    is SuspendListener -> listener.onEvent(event)

                    // suspend the coroutine to run blocking logic
                    //while suspended avoid consuming large amounts
                    //of resources.
                    is EventListener -> suspendCoroutine<Unit> { cont ->
                        cont.resumeWith(runCatching { listener.onEvent(event) })
                    }
                }
            }
        }

        if(event is ShutdownEvent) {
            isShutdown = true
            // TODO Replace with contract assertion in 1.3
            val executor = requireNotNull(context.executor as? ExecutorService) {
                "Executor of context was not an ExecutorService!"
            }
            executor.shutdown()
        }
    }

    override fun register(listener: Any) {
        require(listener is EventListener || listener is SuspendListener) {
            "Listener must implement EventListener or SuspendListener!"
        }
        listeners += listener
        Log.debug("Listener registered: $listener")
    }

    override fun unregister(listener: Any) {
        if(listeners.remove(listener)) {
            Log.debug("Listener unregistered: $listener")
        }
    }

    override fun getRegisteredListeners(): List<Any> = listeners.toList()

    companion object {
        val Log = createLogger(CoroutineEventManager::class)
    }
}
