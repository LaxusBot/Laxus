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
class GoogleImageAPI {
    private companion object {
        private const val URL_FORMAT = "https://www.google.com/search?site=imghp&tbm=isch&" +
                                       "source=hp&biw=1680&bih=940&q=%s&safe=active"
        private const val ENCODING   = "UTF-8"
        private const val USER_AGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64) " +
                                       "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                       "Chrome/53.0.2785.116 Safari/537.36"
        private val LOG = createLogger(GoogleImageAPI::class)
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

            document.select("div.rg_meta").stream()
                .filter { it.childNodeSize() > 0 }
                .map {
                    try {
                        val node = it.childNode(0).toString()
                        val frontIndex = node.indexOf("\"ou\":") + 6 // Find the front index of the json key
                        return@map decode(node.substring(frontIndex, node.indexOf("\",", frontIndex)), ENCODING)
                    } catch (e: UnsupportedOperationException) {
                        LOG.error("An exception was thrown while decoding an image URL: $e")
                    } catch (e: IndexOutOfBoundsException) {
                        LOG.error("An exception was thrown due to improper indexing: $e")
                    }
                    return@map ""
                }
                .filter { it.isNotBlank() }
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
                referrer("https://google.com/")
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
