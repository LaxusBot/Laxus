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

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.SearchListResponse
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import xyz.laxus.util.createLogger
import java.io.IOException
import java.net.InetAddress
import java.time.OffsetDateTime
import java.time.OffsetDateTime.*
import kotlin.coroutines.experimental.suspendCoroutine
import kotlin.streams.toList

/**
 * @author Kaidan Gustave
 */
class YouTubeAPI(private val apiKey: String?) {
    private companion object {
        private const val maxSearchResults = 20L
        private val ytLog = createLogger(YouTubeAPI::class)
        private val netTransport = NetHttpTransport()
        private val jsonFactory = JacksonFactory.getDefaultInstance()
        private val hostAddress = InetAddress.getLocalHost().hostAddress
    }

    private val cache = HashMap<String, Pair<List<String>, OffsetDateTime>>()
    private val mutex = Mutex(locked = false)

    private val isEnabled = apiKey !== null
    private val youtube = YouTube.Builder(netTransport, jsonFactory, {}).setApplicationName("NightFury").build()
    private val search: YouTube.Search.List? = if(!isEnabled) null else try {
        youtube.search().list("id,snippet")
    } catch (e: IOException) {
        ytLog.error("Failed to initialize search: $e")
        null
    }

    suspend fun search(query: String): List<String>? {
        if(search === null) {
            if(isEnabled) {
                ytLog.warn("YouTube searcher initialization failed, search could not be performed!")
            }
            return null
        }
        getCached(query)?.let { return it }
        with(search) {
            q = query
            maxResults = maxSearchResults
            key = apiKey
            userIp = hostAddress
            type = "video"
            fields = "items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)"
        }
        val response = try {
            makeRequest(search)
        } catch(e: GoogleJsonResponseException) {
            ytLog.error("Search failure: ${e.message} - ${e.details.message}")
            return null
        }

        val results = response.items.map { it.id.videoId }
        addToCache(query,results)
        return results
    }

    private suspend fun makeRequest(search: YouTube.Search.List): SearchListResponse = suspendCoroutine { cont ->
        try {
            cont.resume(search.execute())
        } catch(t: Throwable) {
            cont.resumeWithException(t)
        }
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

    fun clean() {
        val now = now()
        synchronized(cache) {
            cache.keys.stream().filter { now.isAfter(cache[it]!!.second.plusHours(2)) }
                .toList().forEach { cache.remove(it) }
        }
    }
}
