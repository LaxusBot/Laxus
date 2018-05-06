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
package xyz.laxus.api.spark.context

import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.html.HTML
import kotlinx.html.HtmlTagMarker
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import me.kgustave.json.JSObject

/**
 * @author Kaidan Gustave
 */
@ContextDsl
class RouteContext(request: spark.Request, response: spark.Response) {
    val request = Request(this, request)
    val response = Response(this, response)

    private val channel = Channel<Any>(1)

    suspend fun send(any: Any) {
        if(channel.isClosedForSend || channel.isFull)
            return
        return channel.send(any)
    }

    suspend fun receive(): Any {
        if(channel.isClosedForReceive || channel.isEmpty)
            return ""
        return channel.receive()
    }

    suspend inline fun sendJson(block: JSObject.() -> Unit) = send(JSObject(block))

    @HtmlTagMarker
    suspend inline fun sendHtml(crossinline block: HTML.() -> Unit) =
        send(createHTML(prettyPrint = false).html { block() })

    suspend fun finish() {
        if(channel.isFull) return
        channel.send("") // Send empty body
    }
}