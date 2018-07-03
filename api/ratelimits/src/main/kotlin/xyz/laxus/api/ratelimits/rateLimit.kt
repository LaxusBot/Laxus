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
@file:Suppress("Unused")
package xyz.laxus.api.ratelimits

import io.ktor.application.ApplicationCall
import io.ktor.application.feature
import java.time.temporal.Temporal

/**
 * Data necessary to handle a rate-limited route.
 *
 * @param key The identifier for the call that generated this RateLimit.
 * @param uses The current number of on this RateLimit.
 * @param limit The maximum number of uses possible.
 * @param reset The time this RateLimit was last reset.
 * @param exceeded Whether or not this RateLimit has been exceeded.
 */
data class RateLimit(
    val key: String,
    val uses: Int,
    val limit: Int,
    val reset: Temporal,
    val exceeded: Boolean = false
) {
    val remaining get() = limit - uses

    init {
        require(limit > 0) { "Cannot rate limit route with a limit less than 1!" }
    }

    /**
     * Constructs a new RateLimit with 0 uses.
     *
     * @param key The identifier for the call that generated this RateLimit.
     * @param limit The maximum number of uses possible.
     * @param reset The time this RateLimit was last reset.
     */
    constructor(key: String, limit: Int, reset: Temporal): this(key, 0, limit, reset)
}

val ApplicationCall.rateLimit: RateLimit? get() {
    val feature = application.feature(RateLimits)
    val key = feature.controller.selectKey(this)
    return feature.controller.retrieve(this, key)
}
