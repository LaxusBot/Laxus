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
package xyz.laxus.bot.requests.google

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.userAgent
import kotlinx.coroutines.withTimeout
import org.jsoup.Jsoup
import xyz.laxus.commons.collections.concurrentHashMap
import xyz.laxus.utils.createLogger
import xyz.laxus.utils.decodeUrl
import xyz.laxus.utils.encodeUrl
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.util.concurrent.TimeUnit

class GoogleRequester(private val client: HttpClient) {
    private companion object {
        private const val UrlFormat = "https://www.google.com/search?q=%s&num=10"
        private const val UserAgent = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
        private val Log = createLogger(GoogleRequester::class)
    }

    private val cache = concurrentHashMap<String, Pair<List<String>, OffsetDateTime>>()

    suspend fun search(query: String): List<String>? {
        fromCache(query)?.let { return it }
        val request = runCatching { UrlFormat.format(encodeUrl(query)) }.getOrElse {
            Log.error("Error processing request: $it")
            return null
        }

        val results = try {
            val response = withTimeout(7500, TimeUnit.MILLISECONDS) {
                client.get<HttpResponse>(request) { userAgent(UserAgent) }
            }

            Jsoup.parse(response.readText()).select("a[href]").asSequence()
                .map { it.attr("href") }
                .filter { it.startsWith("/url?q=") }
                .mapNotNull { q ->
                    runCatching<String> { decodeUrl(q.substring(7, q.indexOf("&sa="))) }.getOrNull()
                }
                .filter { it.isNotBlank() && it != "/settings/ads/preferences?hl=en" }
                .toList()
        } catch(t: Throwable) {
            Log.error("Caught an exception: $t")
            return null
        }
        cache[query] = results to now()
        return results
    }

    private fun fromCache(query: String): List<String>? {
        val entry = cache[query] ?: return null
        if(now().isAfter(entry.second.plusHours(6))) {
            cache -= query
            return null
        }
        return entry.first
    }

    fun clean() {
        val now = now()
        synchronized(cache) {
            cache.entries
                .filter { now.isAfter(it.value.second.plusHours(2)) }
                .forEach { cache -= it.key }
        }
    }
}
