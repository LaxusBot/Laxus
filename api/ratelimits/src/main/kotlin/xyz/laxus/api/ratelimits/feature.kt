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
@file:Suppress("MemberVisibilityCanBePrivate", "Unused")
package xyz.laxus.api.ratelimits

import io.ktor.application.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.pipeline.ContextDsl
import io.ktor.pipeline.Pipeline
import io.ktor.pipeline.PipelinePhase
import io.ktor.response.header
import io.ktor.routing.*
import io.ktor.util.AttributeKey
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlin.math.min

class RateLimitPipeline: Pipeline<RateLimit, ApplicationCall>(ApplyRateLimitHeaders, RespondToRateLimit) {
    companion object Phases {
        val ApplyRateLimitHeaders = PipelinePhase("Apply RateLimit Headers")
        val RespondToRateLimit = PipelinePhase("Respond To RateLimit")
    }
}

/**
 * Installable feature for ktor server applications.
 *
 * Installation of this feature allows routes to automatically
 * respond to incoming requests
 */
class RateLimits private constructor(configuration: Configuration) {
    internal val controller = configuration.controller

    private val limitHeader = configuration.xRateLimit.limitHeader
    private val remainingHeader = configuration.xRateLimit.remainingHeader
    private val resetHeader = configuration.xRateLimit.resetHeader

    val pipeline = RateLimitPipeline()

    init {
        pipeline.intercept(RateLimitPipeline.ApplyRateLimitHeaders) {
            val rateLimit = subject
            with(call.response) {
                // Retry-After Header
                val rateLimitDuration = Duration.between(call.attributes[instantKey], rateLimit.reset)
                header(HttpHeaders.RetryAfter, "${rateLimitDuration.toMillis()}")

                // X-RateLimit-___ headers
                limitHeader?.let { header(it, rateLimit.limit) }
                remainingHeader?.let { header(it, rateLimit.remaining) }
                resetHeader?.let {
                    val instant = rateLimit.reset as? Instant ?: Instant.from(rateLimit.reset)
                    header(resetHeader, instant.toEpochMilli())
                }
            }
            proceed()
        }
    }

    internal fun interceptPipeline(parent: Route, limit: Int, reset: Duration): Route {
        require(limit > 0) { "Cannot rate limit route with a limit less than 1!" }

        // Create a child route to use as our intercepted pipeline
        val child = parent.createChild(Selector(parent, limit, reset))

        // Insert a phase before the actual call logic
        child.insertPhaseBefore(ApplicationCallPipeline.Call, ApplicationCallPipeline.RateLimit)

        // Register an interceptor on the newly inserted phase
        child.intercept(ApplicationCallPipeline.RateLimit) {
            // The key selector
            val key = controller.selectKey(call)

            // get now since we're most likely going to be using it in any case.
            val now = Instant.now()
            call.attributes.put(instantKey, now)

            // first try to retrieve an existing one
            val rateLimit =
                controller.retrieve(call, key)
                    // if it's reset hasn't passed yet, we'll proceed,
                    //otherwise we'll create a new ratelimit
                    ?.takeIf { (it.reset as? Instant ?: Instant.from(it.reset)).isAfter(now) }
                    // Copy the ratelimit, incrementing it by 1 use.
                    //If we exceed the ratelimit, we also need to mark it as exceeded.
                    // Note that "exceeded" should only be true if we are already
                    //meeting the limit upon copying this RateLimit instance.
                    ?.let { it.copy(uses = min(it.uses + 1, it.limit), exceeded = it.uses >= it.limit) } ?:
                run { RateLimit(key, 1, limit, now.plus(reset)) }

            val t = Instant.from(rateLimit.reset).atOffset(ZoneOffset.UTC)
            application.log.debug("${rateLimit.key} [${rateLimit.uses}/${rateLimit.limit}] resets at " +
                                  DateTimeFormatter.RFC_1123_DATE_TIME.format(t))

            // store the ratelimit
            controller.store(call, key, rateLimit)

            pipeline.execute(call, rateLimit)

            if(rateLimit.exceeded) {
                // set status to 429 - Too Many Requests
                call.response.status(HttpStatusCode.TooManyRequests)
                controller.onExceed(call, rateLimit)
                finish()
            } else {
                // proceed
                proceed()
            }
        }

        return child
    }

    class Configuration internal constructor(configure: Configuration.() -> Unit) {
        internal val xRateLimit = XRateLimit()

        var controller: RateLimitController = MapRateLimitController()

        init { this.configure() }

        fun xRateLimitHeaders(configure: XRateLimit.() -> Unit) = xRateLimit.configure()

        class XRateLimit internal constructor() {
            internal var limitHeader = null as String?
            internal var remainingHeader = null as String?
            internal var resetHeader = null as String?

            fun limit(header: String = XRateLimitLimit) {
                this.limitHeader = header
            }

            fun remaining(header: String = XRateLimitRemaining) {
                this.remainingHeader = header
            }

            fun reset(header: String = XRateLimitReset) {
                this.resetHeader = header
            }
        }
    }

    private data class Selector(val route: Route, val limit: Int, val reset: Duration):
        RouteSelector(RouteSelectorEvaluation.qualityConstant) {
        override fun evaluate(context: RoutingResolveContext, segmentIndex: Int): RouteSelectorEvaluation {
            return RouteSelectorEvaluation.Constant
        }
    }

    private class RateLimitPipelineException: Throwable()

    companion object Feature: ApplicationFeature<Application, Configuration, RateLimits> {
        private const val XRateLimitLimit = "X-RateLimit-Limit"
        private const val XRateLimitReset = "X-RateLimit-Reset"
        private const val XRateLimitRemaining = "X-RateLimit-Remaining"

        // internal key used to store the instant we start processing the rate-limit
        private val instantKey = AttributeKey<Instant>("RateLimit Instant")

        override val key = AttributeKey<RateLimits>("RateLimits")

        override fun install(pipeline: Application, configure: Configuration.() -> Unit) =
            RateLimits(Configuration(configure))
    }
}

@Suppress("ObjectPropertyName")
private val _RateLimit = PipelinePhase("RateLimit")

val ApplicationCallPipeline.ApplicationPhase.RateLimit get() = _RateLimit

/**
 * Creates a [rate limited][RateLimits] block where all child routes of the [build]
 * function are rate-limited.
 *
 * @receiver The parent [Route].
 * @param limit The maximum number of uses before child routes respond with [429][HttpStatusCode].
 * @param seconds The number of seconds that must elapse for child routes to reset.
 * @param build The function to build routes under the rate-limit with.
 *
 * @return The resulting [Route].
 */
@ContextDsl
fun Route.rateLimit(limit: Int, seconds: Int, build: Route.() -> Unit): Route =
    rateLimit(limit, Duration.ofSeconds(seconds.toLong()), build)

/**
 * Creates a [rate limited][RateLimits] block where all child routes of the [build]
 * function are rate-limited.
 *
 * @receiver The parent [Route].
 * @param limit The maximum number of uses before child routes respond with [429][HttpStatusCode].
 * @param length The time that must elapse for child routes to reset.
 * @param unit The unit of which the [length] is measured in.
 * @param build The function to build routes under the rate-limit with.
 *
 * @return The resulting [Route].
 */
@ContextDsl
fun Route.rateLimit(limit: Int, length: Long, unit: TimeUnit, build: Route.() -> Unit): Route =
    rateLimit(limit, Duration.of(length, chronoUnitOf(unit)), build)

/**
 * Creates a [rate limited][RateLimits] block where all child routes of the [build]
 * function are rate-limited.
 *
 * @receiver The parent [Route].
 * @param limit The maximum number of uses before child routes respond with [429][HttpStatusCode].
 * @param reset The duration that must elapse for child routes to reset.
 * @param build The function to build routes under the rate-limit with.
 *
 * @return The resulting [Route].
 */
@ContextDsl
fun Route.rateLimit(limit: Int, reset: Duration, build: Route.() -> Unit): Route =
    application.feature(RateLimits).interceptPipeline(this, limit, reset).also(build)
