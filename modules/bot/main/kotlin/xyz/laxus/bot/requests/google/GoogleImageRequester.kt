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
import io.ktor.client.request.header
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readText
import io.ktor.http.HttpHeaders
import io.ktor.http.userAgent
import kotlinx.coroutines.withTimeout
import org.jsoup.Jsoup
import xyz.laxus.commons.collections.concurrentHashMap
import xyz.laxus.utils.createLogger
import xyz.laxus.utils.decodeUrl
import xyz.laxus.utils.encodeUrl
import java.io.IOException
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.util.concurrent.TimeUnit

class GoogleImageRequester(private val client: HttpClient) {
    private companion object {
        private const val UrlFormat = "https://www.google.com/search?site=imghp&tbm=isch&" +
                                      "source=hp&biw=1680&bih=940&q=%s&safe=active"
        private const val UserAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64) " +
                                      "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                      "Chrome/53.0.2785.116 Safari/537.36"
        private val Log = createLogger(GoogleImageRequester::class)
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
                client.get<HttpResponse>(request) {
                    userAgent(UserAgent)
                    header(HttpHeaders.Referrer, "https://google.com/")
                }
            }

            Jsoup.parse(response.readText()).select("div.rg_meta").asSequence()
                .filter { it.childNodeSize() > 0 }
                .mapNotNull map@ {
                    try {
                        val node = it.childNode(0).toString()
                        val frontIndex = node.indexOf("\"ou\":") + 6 // Find the front index of the json key
                        return@map decodeUrl(node.substring(frontIndex, node.indexOf("\",", frontIndex)))
                    } catch (e: UnsupportedOperationException) {
                        Log.error("An exception was thrown while decoding an image URL: $e")
                    } catch (e: IndexOutOfBoundsException) {
                        Log.error("An exception was thrown due to improper indexing: $e")
                    }
                    return@map null
                }
                .filter { it.isNotBlank() }
                .toList()
        } catch(e: IOException) {
            Log.error("Encountered an IOException: $e")
            return null
        } catch(t: Throwable) {
            Log.error("Caught an exception: $t")
            return null
        }
        cache[query] = results to now()
        return results
    }

    private fun fromCache(query: String): List<String>? {
        val entry = cache[query] ?: return null
        if(now().isAfter(entry.second.plusHours(4))) {
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
