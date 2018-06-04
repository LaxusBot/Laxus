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
@file:JvmName("SparkCompatUtil")
package xyz.laxus.wyvern.internal.websocket

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketListener
import xyz.laxus.wyvern.websocket.IWebSocket

internal fun IWebSocket.asWebSocketListener(): WebSocketListener = SparkWebSocketWrapper(this)

private class SparkWebSocketWrapper(private val ws: IWebSocket): WebSocketListener {
    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) = ws.onBinary(payload, offset, len)
    override fun onWebSocketClose(statusCode: Int, reason: String?) = ws.onClose(statusCode, reason)
    override fun onWebSocketConnect(session: Session) = ws.onConnect(session)
    override fun onWebSocketError(cause: Throwable) = ws.onError(cause)
    override fun onWebSocketText(message: String) = ws.onText(message)
    override fun hashCode() = ws.hashCode()
    override fun equals(other: Any?) = ws == other
    override fun toString() = ws.toString()
}