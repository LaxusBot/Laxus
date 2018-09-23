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

package xyz.laxus.bot.requests.dbots

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.response.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.withCharset
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.User
import xyz.laxus.bot.utils.jda.size
import xyz.laxus.utils.onlySupport

class DiscordBotsRequester(private val client: HttpClient, private val apiKey: String?) {
    val usable = apiKey !== null

    suspend fun getBotInfo(bot: User): DiscordBotsInfo = getBotInfo(bot.idLong)
    suspend fun getBotInfo(id: Long): DiscordBotsInfo {
        onlySupport(apiKey != null) { "API key must be specified to use requester!" }
        val response = client.get<HttpResponse>("$BaseApiUrl/bots/$id") {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            header(HttpHeaders.Authorization, apiKey)
        }

        checkResponse(response)
        return response.receive()
    }

    suspend fun postStats(jda: JDA) =
        postStats(jda.selfUser.idLong, DiscordBotsStats(jda.shardInfo, jda.guildCache.size))
    suspend fun postStats(id: Long, stats: DiscordBotsStats) {
        onlySupport(apiKey != null) { "API key must be specified to use requester!" }
        val response = client.post<HttpResponse>("$BaseApiUrl/bots/$id/stats") {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            header(HttpHeaders.Authorization, apiKey)
            body = stats
        }

        checkResponse(response)
    }

    suspend fun getStats(jda: JDA): DiscordBotsStats.Info = getStats(jda.selfUser.idLong)
    suspend fun getStats(id: Long): DiscordBotsStats.Info {
        onlySupport(apiKey != null) { "API key must be specified to use requester!" }
        val response = client.get<HttpResponse>("$BaseApiUrl/bots/$id/stats") {
            contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
            header(HttpHeaders.Authorization, apiKey)
        }

        checkResponse(response)
        return response.receive()
    }

    private companion object {
        private const val BaseApiUrl = "https://bots.discord.pw/api"

        private suspend fun checkResponse(response: HttpResponse) {
            response.status.value.takeIf { it >= 400 }?.let { code ->
                throw DiscordBotsError(response.receive<JsonObject>()["error"].content, code)
            }
        }
    }
}
