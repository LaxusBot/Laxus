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
package xyz.laxus.api.spark.context

import kotlinx.html.HTML
import kotlinx.html.HtmlTagMarker
import me.kgustave.json.JSObject
import xyz.laxus.api.util.ContentType
import xyz.laxus.api.util.HttpStatusCode

/**
 * @author Kaidan Gustave
 */
@ContextDsl
class Response(val context: RouteContext, private val base: spark.Response) {
    var status: HttpStatusCode
        get() = status()
        set(value) = status(value)

    fun status(): HttpStatusCode {
        return HttpStatusCode.codeOf(base.status())
    }

    fun status(status: Int) {
        base.status(status)
    }

    fun status(status: HttpStatusCode) {
        base.status(status.code)
    }

    fun contentType(value: ContentType) {
        base.header("Content-Type", "$value")
    }

    fun header(header: String, value: String) {
        base.header(header, value)
    }

    fun redirect(location: String, status: HttpStatusCode = status()) {
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