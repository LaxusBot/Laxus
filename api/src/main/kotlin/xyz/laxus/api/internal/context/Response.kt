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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.laxus.api.internal.context

import kotlinx.html.HTML
import kotlinx.html.HtmlTagMarker
import me.kgustave.json.JSObject
import org.eclipse.jetty.http.HttpStatus
import xyz.laxus.api.util.ContentType

/**
 * @author Kaidan Gustave
 */
@ContextDsl
class Response(val context: RouteContext, private val base: spark.Response) {
    var status: HttpStatus.Code
        get() = status()
        set(value) = status(value)

    fun status(): HttpStatus.Code = checkNotNull(HttpStatus.getCode(base.status()))

    fun status(status: Int) {
        base.status(status)
    }

    fun status(status: HttpStatus.Code) {
        status(status.code)
    }

    fun header(header: String, value: String?) {
        base.raw().setHeader(header, value)
    }

    fun contentType(value: ContentType?) {
        header("Content-Type", value?.let { "$it" })
    }

    fun redirect(location: String, status: HttpStatus.Code = status()) {
        base.redirect(location, status.code)
    }

    @ContextDsl
    suspend inline fun respondJson(block: JSObject.() -> Unit) {
        contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
        return context.sendJson(block)
    }

    @ContextDsl
    @HtmlTagMarker
    suspend inline fun respondHtml(crossinline block: HTML.() -> Unit) {
        contentType(ContentType.Text.Html.withCharset(Charsets.UTF_8))
        return context.sendHtml(block)
    }
}