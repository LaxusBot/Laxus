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
import io.ktor.pipeline.Pipeline
import io.ktor.pipeline.PipelinePhase

/**
 * Execution pipeline for [RateLimits] feature.
 */
class RateLimitPipeline internal constructor():
    Pipeline<RateLimit, ApplicationCall>(StoreRateLimit, AppendRateLimitHeaders, OnExceededRateLimit) {

    /**
     * Phases for the [RateLimitPipeline].
     *
     * Order of phases:
     * 1) [StoreRateLimit]
     * 2) [AppendRateLimitHeaders]
     * 3) [OnExceededRateLimit]
     *
     * Note that unlike common pipeline's in the ktor library,
     * this one has slightly specific behavior in order to
     * maintain a solid flow of execution.
     *
     * Details of individual phases is in the documentation for each phase.
     */
    companion object Phases {
        /**
         * Phase where [RateLimit] is [stored][RateLimitController.store].
         */
        val StoreRateLimit = PipelinePhase("Store RateLimit")

        /**
         * Phase where headers are applied to the [call][ApplicationCall].
         *
         * Pipeline interceptions after this phase will not be processed if the
         * [subject][io.ktor.pipeline.PipelineContext.subject] [RateLimit] has
         * not [exceeded][RateLimit.exceeded].
         *
         * Also note that this phase [finishes][io.ktor.pipeline.PipelineContext.finish]
         * the pipeline's execution early if the subject RateLimit has exceeded, and will
         * skip any additional phases installed after this phase.
         */
        val AppendRateLimitHeaders = PipelinePhase("Append RateLimit Headers")

        /**
         * Phase where [RateLimit] is exceeded.
         *
         * Interceptions of this phase can only be made **before** this,
         * and any interception after this will not be processed.
         *
         * With that, phases added to this pipeline after this phase
         * will **not** be processed.
         */
        val OnExceededRateLimit = PipelinePhase("On Exceeded RateLimit")
    }

    override fun toString() =
        "RateLimitPipeline${items.joinToString(", ", "(", ")", transform = PipelinePhase::name)}"
}