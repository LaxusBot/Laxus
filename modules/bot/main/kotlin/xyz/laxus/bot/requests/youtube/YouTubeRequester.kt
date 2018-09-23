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
package xyz.laxus.bot.requests.youtube

import io.ktor.client.HttpClient
import io.ktor.client.call.receive
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.response.HttpResponse
import io.ktor.http.URLProtocol
import xyz.laxus.commons.collections.concurrentHashMap
import xyz.laxus.utils.onlySupport
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now

class YouTubeRequester(private val client: HttpClient, private val apiKey: String?) {
    private val cache = concurrentHashMap<String, Pair<SearchListResponse, OffsetDateTime>>()

    suspend fun search(query: String, maxResults: Int = 10): SearchListResponse {
        onlySupport(apiKey != null) { "API key was not specified!" }
        require(query.isNotBlank()) { "Query is blank!" }
        require(maxResults > 0) { "Max Results must be a positive non-zero number!" }

        fromCache(query)?.let { return it }

        val response = client.get<HttpResponse> {
            url {
                protocol = URLProtocol.HTTPS
                host = "www.googleapis.com"
                path("youtube", "v3", "search")

                parameter("key", apiKey)
                parameter("part", "snippet")
                parameter("type", "video")
                parameter("q", query)
                parameter("maxResults", "$maxResults")
            }
        }

        check(response.status.value < 400) { "Received an error response: ${response.status}" }

        val result = response.receive<SearchListResponse>()
        cache[query] = result to now()
        return result
    }

    private fun fromCache(query: String): SearchListResponse? {
        val entry = cache[query] ?: return null
        if(now().isAfter(entry.second.plusHours(2))) {
            cache -= query
            return null
        }
        return entry.first
    }
}
