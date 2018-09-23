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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import xyz.laxus.bot.requests.google.GoogleImageRequester
import xyz.laxus.bot.requests.google.GoogleRequester
import xyz.laxus.bot.requests.ktor.RequestSerializer
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(PER_CLASS)
class GoogleTests {
    private val client = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = RequestSerializer()
        }
    }

    @Nested @TestInstance(PER_CLASS) inner class GoogleSearchTests {
        private val google = GoogleRequester(client)

        @Test fun `Test Google Search`() = runBlocking {
            val results = google.search("cats")
            assertNotNull(results)
            for(result in results) {
                assertTrue(result.startsWith("http"))
            }
        }
    }

    @Nested @TestInstance(PER_CLASS) inner class GoogleImagesTests {
        private val images = GoogleImageRequester(client)

        @Test fun `Test Google Image Search`() = runBlocking {
            val results = images.search("dogs")
            assertNotNull(results)
            for(result in results) {
                assertTrue(result.startsWith("http"))
            }
        }
    }
}
