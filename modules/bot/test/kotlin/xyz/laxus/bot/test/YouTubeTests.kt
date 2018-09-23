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
package xyz.laxus.bot.test

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import xyz.laxus.bot.requests.ktor.RequestSerializer
import xyz.laxus.bot.requests.youtube.YouTubeRequester
import xyz.laxus.config.loadConfig
import xyz.laxus.config.string
import xyz.laxus.testing.extensions.EnabledIfResourcePresent
import java.time.Month
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import java.time.LocalDateTime.parse as parseLocalDateTime
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME as ISO8601

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfResourcePresent(named = "/youtube.conf")
class YouTubeTests {
    private val config = loadConfig("/youtube.conf")
    private val client = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = RequestSerializer()
        }
    }
    private val youtube = YouTubeRequester(client, config.string("api.key"))

    @Test fun `Test Search`() = runBlocking {
        val result = youtube.search("kotlin")

        assertNotNull(result.etag)
        assertEquals("youtube#searchListResponse", result.kind)
        assertEquals(10, result.items.size)
    }

    @Test fun `Test Specific Search`() = runBlocking {
        val result = youtube.search("who do u think u r Kaitlyn K")
        val selected = result.items[0]
        val id = selected.id?.videoId
        val title = selected.snippet?.title
        val channel = selected.snippet?.channelTitle
        val time = parseLocalDateTime(selected.snippet?.publishedAt, ISO8601)

        assertEquals("Sxj8nDQPPO0", id)
        assertEquals("'Who Do U Think U R?' (Song & Lyrics) - Kaitlyn K", title)
        assertEquals("Kaitlyn K", channel)
        assertEquals(2, time.dayOfMonth)
        assertEquals(Month.APRIL, time.month)
    }

    @AfterAll fun `Destroy HttpClient`() {
        client.close()
    }
}
