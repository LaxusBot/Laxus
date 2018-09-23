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
package xyz.laxus.bot.requests.ktor

import io.ktor.client.call.TypeInfo
import io.ktor.client.features.json.JsonSerializer
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.client.utils.EmptyContent
import io.ktor.http.ContentType
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonTreeParser
import kotlinx.serialization.serializer

class RequestSerializer: JsonSerializer {
    override suspend fun read(type: TypeInfo, response: HttpResponse): Any {
        val text = response.readText(response.charset() ?: Charsets.UTF_8)
        return when(val t = type.type) {
            JsonObject::class, JsonArray::class -> JsonTreeParser(text).readFully()
            else -> JSON.parse(t.serializer(), text)
        }
    }

    override fun write(data: Any): OutgoingContent {
        if(data is EmptyContent) return EmptyContent
        val text = when(data) {
            is JsonObject, is JsonArray -> data.toString()
            else ->
                @Suppress("UNCHECKED_CAST")
                JSON.stringify(data::class.serializer() as KSerializer<Any>, data)
        }
        return TextContent(text, ContentType.Application.Json)
    }
}
