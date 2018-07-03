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
package xyz.laxus.api.ratelimits

import io.ktor.application.ApplicationCall
import io.ktor.features.origin
import io.ktor.http.HttpStatusCode
import io.ktor.request.host
import io.ktor.response.respond

/**
 * A controller for [RateLimit] instances.
 *
 * @author Kaidan Gustave
 */
interface RateLimitController {
    fun expire(call: ApplicationCall, key: String)
    fun retrieve(call: ApplicationCall, key: String): RateLimit?
    fun store(call: ApplicationCall, key: String, rateLimit: RateLimit)

    fun selectKey(call: ApplicationCall): String {
        val request = call.request
        return request.local.host + " on " + request.origin.uri.substringBefore('?')
    }

    suspend fun onExceed(call: ApplicationCall, rateLimit: RateLimit) {
        call.respond(HttpStatusCode.TooManyRequests)
    }
}

internal class MapRateLimitController: RateLimitController {
    private val rateLimits = mutableMapOf<String, RateLimit>()

    override fun expire(call: ApplicationCall, key: String) {
        rateLimits -= key
    }

    override fun retrieve(call: ApplicationCall, key: String): RateLimit? {
        return rateLimits[key]
    }

    override fun store(call: ApplicationCall, key: String, rateLimit: RateLimit) {
        rateLimits[key] = rateLimit
    }
}