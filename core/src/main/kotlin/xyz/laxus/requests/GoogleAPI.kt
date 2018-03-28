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
package xyz.laxus.requests

import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import xyz.laxus.util.createLogger
import xyz.laxus.util.ignored
import java.io.IOException
import java.net.URLDecoder.*
import java.net.URLEncoder.*
import java.time.OffsetDateTime
import java.time.OffsetDateTime.*
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class GoogleAPI {
    private companion object {
        private const val URL_FORMAT = "https://www.google.com/search?q=%s&num=10"
        private const val ENCODING = "UTF-8"
        private const val USER_AGENT = "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"
        private val LOG = createLogger(GoogleAPI::class)
    }

    private val cache = HashMap<String, Pair<List<String>, OffsetDateTime>>()
    private val mutex = Mutex(locked = false)

    suspend fun search(query: String): List<String>? {
        getCached(query)?.let { return it }
        val request = try {
            URL_FORMAT.format(encode(query, ENCODING))
        } catch (e: UnsupportedOperationException) {
            LOG.error("Error processing request: $e")
            return null
        }

        val results = try {
            val document = makeRequest(request)

            document.select("a[href]").stream()
                .map    { it.attr("href") }
                .filter { it.startsWith("/url?q=") }
                .map    { ignored("") { decode(it.substring(7, it.indexOf("&sa=")), ENCODING) } }
                .filter { it.isNotBlank() && it != "/settings/ads/preferences?hl=en" }
                .toList()
        } catch(e: IOException) {
            LOG.error("Encountered an IOException: $e")
            return null
        } catch(t: Throwable) {
            LOG.error("Caught an exception: $t")
            return null
        }
        addToCache(query, results)
        return results
    }

    private suspend fun getCached(query: String): List<String>? {
        return mutex.withLock(cache) {
            cache[query]?.first
        }
    }

    private suspend fun addToCache(query: String, results: List<String>) {
        mutex.withLock(cache) {
            cache[query] = results to now()
        }
    }

    private suspend fun makeRequest(request: String): Document = suspendCoroutine { cont ->
        try {
            val document = Jsoup.connect(request).apply {
                userAgent(USER_AGENT)
                timeout(7500)
            }.get()
            requireNotNull(document) { "Document retrieved for request '$request' was null!" }
            cont.resume(document)
        } catch(t: Throwable) {
            cont.resumeWithException(t)
        }
    }

    fun clean() {
        val now = now()
        synchronized(cache) {
            cache.keys.stream().filter { now.isAfter(cache[it]!!.second.plusHours(2)) }
                .toList().forEach { cache.remove(it) }
        }
    }
}
