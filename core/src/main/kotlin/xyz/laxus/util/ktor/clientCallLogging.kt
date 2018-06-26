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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.laxus.util.ktor

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.features.HttpClientFeature
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.response.HttpReceivePipeline
import io.ktor.client.response.HttpResponse
import io.ktor.util.AttributeKey
import org.slf4j.event.Level
import xyz.laxus.util.createLogger
import kotlin.reflect.KClass

class ClientCallLogging private constructor(
    name: String,
    val send: PipelineConfiguration,
    val receive: PipelineConfiguration,
    val error: PipelineConfiguration
) {
    private val log = createLogger(name)

    private fun logSend(msg: String) = logAt(send.level, msg)
    private fun logReceive(msg: String) = logAt(receive.level, msg)
    private fun logError(msg: String) = logAt(error.level, msg)
    private fun logAt(level: Level, msg: String) {
        when(level) {
            Level.TRACE -> log.trace(msg)
            Level.DEBUG -> log.debug(msg)
            Level.INFO -> log.info(msg)
            Level.WARN -> log.warn(msg)
            Level.ERROR -> log.error(msg)
        }
    }

    class Configuration internal constructor() {
        private lateinit var name: String
        val send = PipelineConfiguration()
        val receive = PipelineConfiguration()
        val error = PipelineConfiguration()

        @ClientCallLoggingDsl
        fun name(name: String) {
            this.name = name
        }

        @ClientCallLoggingDsl
        fun name(klass: KClass<*>) {
            val name = requireNotNull(klass.qualifiedName) {
                "Cannot resolve qualified name for class: $klass"
            }
            name(name)
        }

        init { name(ClientCallLogging::class) }

        internal fun build() = ClientCallLogging(name, send, receive, error)
    }

    data class PipelineConfiguration(var mode: Mode = Mode.SIMPLE, var level: Level = Level.INFO)

    companion object Feature: HttpClientFeature<Configuration, ClientCallLogging> {
        override val key = AttributeKey<ClientCallLogging>("ClientCallLogging")
        override suspend fun prepare(block: Configuration.() -> Unit) = Configuration().also(block).build()
        override fun install(feature: ClientCallLogging, scope: HttpClient) {
            scope.sendPipeline.intercept(HttpSendPipeline.Before) {
                feature.logSend(feature.send.mode.formatRequest(context))
            }

            scope.receivePipeline.intercept(HttpReceivePipeline.After) {
                if(subject.status.value >= 400) {
                    feature.logError(feature.error.mode.formatResponse(subject))
                } else {
                    feature.logReceive(feature.receive.mode.formatResponse(subject))
                }
            }
        }
    }

    enum class Mode {
        SIMPLE, DETAILED;

        fun formatRequest(request: HttpRequestBuilder): String = buildString {
            append("Sending Request: ${request.method.value} - ${request.url.buildString()}")
            if(this@Mode == DETAILED) {
                formatHeaders(request.headers.entries())
            }
        }.trim()

        fun formatResponse(response: HttpResponse): String = buildString {
            append("Received Response: ${response.version} ${response.status.value} ${response.status.description}")
            if(this@Mode == DETAILED) {
                formatHeaders(response.headers.entries())
            }
        }.trim()

        private fun StringBuilder.formatHeaders(headers: Set<Map.Entry<String, List<String>>>) {
            appendln()
            for((header, values) in headers) {
                for(value in values) {
                    appendln("$header: $value")
                }
            }
        }
    }
}

@DslMarker
private annotation class ClientCallLoggingDsl

@ClientCallLoggingDsl
inline fun ClientCallLogging.Configuration.send(block: ClientCallLogging.PipelineConfiguration.() -> Unit) = send.block()
@ClientCallLoggingDsl
inline fun ClientCallLogging.Configuration.receive(block: ClientCallLogging.PipelineConfiguration.() -> Unit) = receive.block()
@ClientCallLoggingDsl
inline fun ClientCallLogging.Configuration.error(block: ClientCallLogging.PipelineConfiguration.() -> Unit) = error.block()
@ClientCallLoggingDsl
suspend inline fun HttpClientConfig.logging(
    crossinline block: ClientCallLogging.Configuration.() -> Unit
) = install(ClientCallLogging) { block() }
