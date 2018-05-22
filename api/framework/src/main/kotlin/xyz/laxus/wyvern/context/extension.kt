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
package xyz.laxus.wyvern.context

import kotlinx.html.HTML
import kotlinx.html.HtmlTagMarker
import kotlinx.html.html
import kotlinx.html.stream.createHTML
import me.kgustave.json.JSObject
import xyz.laxus.wyvern.http.header.ContentType
import java.nio.charset.Charset

@ContextDsl
@HtmlTagMarker
suspend inline fun RouteContext.sendHtml(crossinline block: HTML.() -> Unit) =
    send(createHTML(prettyPrint = false).html { block() })

@ContextDsl
suspend inline fun RouteContext.sendJson(block: JSObject.() -> Unit) = send(JSObject(block))

@ContextDsl
@HtmlTagMarker
suspend inline fun Response.respondHtml(
    charset: Charset = Charsets.UTF_8,
    crossinline block: HTML.() -> Unit
) {
    contentType(ContentType.Text.Html.withCharset(charset))
    return context.sendHtml(block)
}

@ContextDsl
suspend inline fun Response.respondJson(
    charset: Charset = Charsets.UTF_8,
    block: JSObject.() -> Unit
) {
    contentType(ContentType.Application.Json.withCharset(charset))
    return context.sendJson(block)
}