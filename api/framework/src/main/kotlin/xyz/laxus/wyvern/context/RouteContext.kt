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
package xyz.laxus.wyvern.context

import kotlinx.coroutines.experimental.channels.Channel

/**
 * @author Kaidan Gustave
 */
@ContextDsl
class RouteContext(request: spark.Request, response: spark.Response, private val suspended: Boolean) {
    val request = Request(this, request)
    val response = Response(this, response)

    private val channel = if(suspended) Channel<Any>(1) else null

    suspend fun send(any: Any) {
        check(suspended) { "Cannot send on un-suspended context!" }
        channel!! // assert non-null
        if(channel.isClosedForSend || channel.isFull)
            return
        return channel.send(any)
    }

    internal suspend fun receive(): Any {
        check(suspended) { "Cannot receive on un-suspended context!" }
        channel!! // assert non-null
        if(channel.isClosedForReceive || channel.isEmpty)
            return ""
        return channel.receive()
    }

    internal suspend fun finish() {
        check(suspended) { "Cannot finish on un-suspended context!" }
        channel!! // assert non-null
        if(channel.isFull) return
        channel.send("") // Send empty body
    }
}