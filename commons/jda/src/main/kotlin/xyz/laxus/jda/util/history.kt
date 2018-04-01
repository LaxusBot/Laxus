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
package xyz.laxus.jda.util

import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.ProducerScope
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.produce
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageHistory
import kotlin.coroutines.experimental.CoroutineContext

interface HistoryScope : ProducerScope<List<Message>> {
    val history: MessageHistory
    val jda: JDA
    val chan: MessageChannel // couldn't name channel because conflicts with ProducerScope
    val size: Int

    fun isEmpty(): Boolean

    suspend fun retrievePast(amount: Int): List<Message>

    suspend fun retrieveFuture(amount: Int): List<Message>

    suspend fun sendPast(amount: Int) {
        val retrieved = retrievePast(amount)
        send(retrieved)
    }

    suspend fun sendFuture(amount: Int) {
        val retrieved = history.retrieveFuture(amount).await()
        send(retrieved)
    }
}

private class HistoryScopeImpl(
    override val history: MessageHistory,
    scope: ProducerScope<List<Message>>
): HistoryScope, ProducerScope<List<Message>> by scope {
    override val jda: JDA get() = history.jda
    override val chan: MessageChannel get() = history.channel
    override val size: Int get() = history.size()

    override fun isEmpty(): Boolean = history.isEmpty

    override suspend fun retrievePast(amount: Int): List<Message> {
        return history.retrievePast(amount).await()
    }

    override suspend fun retrieveFuture(amount: Int): List<Message> {
        return history.retrieveFuture(amount).await()
    }
}

fun MessageChannel.produceHistory(
    context: CoroutineContext = DefaultDispatcher,
    capacity: Int = 0,
    parent: Job? = null,
    block: suspend HistoryScope.() -> Unit
): ReceiveChannel<List<Message>> = produce(context, capacity, parent) {
    val scope = HistoryScopeImpl(history, this)
    scope.block()
}

inline fun MessageChannel.producePast(
    context: CoroutineContext = DefaultDispatcher,
    number: Int,
    retrieveLimit: Int = 100,
    crossinline breakIf: (List<Message>) -> Boolean = { false }
): ReceiveChannel<List<Message>> {
    require(number > 0) { "Minimum of one message must be retrieved" }
    require(retrieveLimit in 1..100) { "Retrieve limit must be inbetween 1 and 100" }
    return produceHistory(context, capacity = number) {
        var left = number
        while(left > retrieveLimit) {
            val retrieved = retrievePast(retrieveLimit)
            left -= retrieveLimit
            if(breakIf(retrieved)) {
                left = 0
                break
            }
            send(retrieved)
        }

        if(left in 1..retrieveLimit) {
            sendPast(left)
        }
        close()
    }
}

inline fun MessageChannel.produceFuture(
    context: CoroutineContext = DefaultDispatcher,
    number: Int,
    retrieveLimit: Int = 100,
    crossinline breakIf: (List<Message>) -> Boolean = { false }
): ReceiveChannel<List<Message>> {
    require(number > 0) { "Minimum of one message must be retrieved" }
    require(retrieveLimit in 1..100) { "Retrieve limit must be inbetween 1 and 100" }
    return produceHistory(context, capacity = number) {
        var left = number
        while(left > retrieveLimit) {
            val retrieved = retrieveFuture(retrieveLimit)
            left -= retrieveLimit
            if(breakIf(retrieved)) {
                left = 0
                break
            }
            send(retrieved)
        }

        if(left in 1..retrieveLimit) {
            sendFuture(left)
        }
        close()
    }
}
