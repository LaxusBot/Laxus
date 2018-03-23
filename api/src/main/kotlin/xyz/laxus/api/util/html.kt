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
package xyz.laxus.api.util

import io.ktor.application.ApplicationCall
import io.ktor.cio.bufferedWriter
import io.ktor.content.OutgoingContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.withCharset
import io.ktor.response.respond
import kotlinx.coroutines.experimental.io.ByteWriteChannel
import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.appendHTML

internal val HtmlContentType = ContentType.Text.Html.withCharset(Charsets.UTF_8)

internal suspend fun ApplicationCall.respondHtml(
    status: HttpStatusCode = HttpStatusCode.OK,
    block: HTML.() -> Unit
) = respond(HtmlContent(status, block))

internal class HtmlContent(
    override val status: HttpStatusCode? = null,
    private val builder: HTML.() -> Unit
): OutgoingContent.WriteChannelContent() {

    override val contentType get() = HtmlContentType

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.bufferedWriter().use {
            it.append("<!DOCTYPE html>\n")
            it.appendHTML().html(builder)
        }
    }
}
