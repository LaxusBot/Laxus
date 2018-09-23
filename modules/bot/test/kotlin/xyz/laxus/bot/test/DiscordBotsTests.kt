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
import kotlinx.serialization.json.JSON
import kotlinx.serialization.json.JsonTreeParser
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import xyz.laxus.bot.requests.dbots.DiscordBotsRequester
import xyz.laxus.bot.requests.dbots.DiscordBotsStats
import xyz.laxus.bot.requests.ktor.RequestSerializer
import xyz.laxus.config.loadConfig
import xyz.laxus.config.string
import xyz.laxus.testing.Slow
import xyz.laxus.testing.extensions.EnabledIfResourcePresent
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DiscordBotsTests {
    @Nested inner class DiscordBotsSerializationTests {
        @Test fun `Test DiscordBotsStats Without Shard Properties Serializes Correctly`() {
            val stats = DiscordBotsStats(serverCount = 423)
            val text = JSON.stringify(stats)
            val keys = JsonTreeParser(text).readFully().jsonObject.keys

            assertFalse("shard_id" in keys)
            assertFalse("shard_total" in keys)
            assertTrue("server_count" in keys)
        }

        @Test fun `Test DiscordBotsStats With Shard Properties Serializes Correctly`() {
            val stats = DiscordBotsStats(shardId = 1, shardTotal = 5, serverCount = 8821)
            val text = JSON.stringify(stats)
            val keys = JsonTreeParser(text).readFully().jsonObject.keys

            assertTrue("shard_id" in keys)
            assertTrue("shard_total" in keys)
            assertTrue("server_count" in keys)
        }
    }

    @EnabledIfResourcePresent("/dbots.conf")
    @Slow @Nested @TestInstance(PER_CLASS) inner class DiscordBotsRequestTests {
        private val apiKey = loadConfig("/dbots.conf").string("api.key")
        private val client = HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = RequestSerializer()
            }
        }
        private val requester = DiscordBotsRequester(client, apiKey)

        @Test fun `Test DiscordBotsInfo From GET Request`() = runBlocking {
            val info = requester.getBotInfo(263895505145298944L)

            assertEquals("JDA", info.library)
            assertEquals("|", info.prefix)
            assertEquals("Laxus", info.name)
        }

        @Test fun `Test DiscordBotsStats From GET Request`() = runBlocking {
            val info = requester.getStats(263895505145298944L)

            assertTrue(info.stats.isNotEmpty())
            assertTrue(info.stats[0].serverCount > 0)
        }

        @AfterAll fun `Destroy HttpClient`() {
            client.close()
        }
    }
}
