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
@file:Suppress("unused", "HasPlatformType")
package xyz.laxus.bot.utils.jda

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageHistory
import kotlin.coroutines.CoroutineContext

private typealias HistoryCheck = suspend (List<Message>) -> Boolean
private typealias MessageReceiver = ReceiveChannel<List<Message>>

inline fun <reified C: MessageChannel> C.historyAt(messageId: Long) = getHistoryAround(messageId, 1)
inline fun <reified C: MessageChannel> C.historyAt(message: Message) = getHistoryAround(message, 1)

interface HistoryScope: ProducerScope<List<Message>> {
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
        val retrieved = retrieveFuture(amount)
        send(retrieved)
    }
}

private class HistoryScopeImpl(
    override val history: MessageHistory,
    private val scope: ProducerScope<List<Message>>
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
    context: CoroutineContext = Dispatchers.Default,
    capacity: Int = 0,
    block: suspend HistoryScope.() -> Unit
): MessageReceiver = history.produceHistory(context, capacity, block)

fun MessageChannel.producePast(
    context: CoroutineContext = Dispatchers.Default,
    number: Int,
    retrieveLimit: Int = 100,
    breakIf: HistoryCheck = { false }
): MessageReceiver = history.producePast(context, number, retrieveLimit, breakIf)

fun MessageChannel.produceFuture(
    context: CoroutineContext = Dispatchers.Default,
    number: Int,
    retrieveLimit: Int = 100,
    breakIf: HistoryCheck = { false }
): MessageReceiver = history.produceFuture(context, number, retrieveLimit, breakIf)

fun MessageHistory.produceHistory(
    context: CoroutineContext = Dispatchers.Default,
    capacity: Int,
    block: suspend HistoryScope.() -> Unit
): MessageReceiver = GlobalScope.produce(context, capacity) {
    val scope = HistoryScopeImpl(this@produceHistory, this)
    scope.block()
}

fun MessageHistory.producePast(
    context: CoroutineContext = Dispatchers.Default,
    number: Int,
    retrieveLimit: Int = 100,
    breakIf: HistoryCheck = { false }
): MessageReceiver {
    require(number > 0) { "Minimum of one message must be retrieved" }
    require(retrieveLimit in 1..100) { "Retrieve limit must be inbetween 1 and 100" }

    return produceHistory(context, capacity = number) {
        // what is left
        var left = number

        // while we are over the retrieve limit
        while(left > retrieveLimit) {
            // retrieve past limit
            val retrieved = retrievePast(retrieveLimit)
            // subtract the limit from what's left
            left -= retrieveLimit
            // if our break condition is met by this retrieval
            if(breakIf(retrieved)) {
                // do not collect any more after the loop
                left = 0
                // break
                break
            }
            // send the retrieved messages
            send(retrieved)
        }

        // if we have any more to retrieve
        if(left in 1..retrieveLimit) {
            // retrieve what is left
            val retrieved = retrievePast(left)
            // if it doesn't meet the break condition
            if(!breakIf(retrieved)) {
                // send it
                send(retrieved)
            }
        }
    }

}

fun MessageHistory.produceFuture(
    context: CoroutineContext = Dispatchers.Default,
    number: Int,
    retrieveLimit: Int = 100,
    breakIf: HistoryCheck = { false }
): MessageReceiver {
    require(number > 0) { "Minimum of one message must be retrieved" }
    require(retrieveLimit in 1..100) { "Retrieve limit must be inbetween 1 and 100" }

    return produceHistory(context, capacity = number) {
        // what is left
        var left = number

        // while we are over the retrieve limit
        while(left > retrieveLimit) {
            // retrieve future limit
            val retrieved = retrieveFuture(retrieveLimit)
            // subtract the limit from what's left
            left -= retrieveLimit
            // if our break condition is met by this retrieval
            if(breakIf(retrieved)) {
                // do not collect any more after the loop
                left = 0
                // break
                break
            }
            // send the retrieved messages
            send(retrieved)
        }

        // if we have any more to retrieve
        if(left in 1..retrieveLimit) {
            // retrieve what is left
            val retrieved = retrieveFuture(left)
            // if it doesn't meet the break condition
            if(!breakIf(retrieved)) {
                // send it
                send(retrieved)
            }
        }
    }
}
