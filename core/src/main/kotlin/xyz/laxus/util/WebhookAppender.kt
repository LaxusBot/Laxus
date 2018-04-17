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
package xyz.laxus.util

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import net.dv8tion.jda.webhook.WebhookClient
import net.dv8tion.jda.webhook.WebhookClientBuilder
import xyz.laxus.Laxus
import xyz.laxus.jda.KEmbedBuilder
import xyz.laxus.jda.util.embed
import java.awt.Color
import java.time.OffsetDateTime
import java.util.*

/**
 * @author Kaidan Gustave
 */
class WebhookAppender: AppenderBase<ILoggingEvent>() {
    private companion object {
        private const val EmbedLimit = 750
        private inline fun WebhookClient.send(embed: KEmbedBuilder.() -> Unit) = send(embed(embed))
    }

    private lateinit var client: WebhookClient
    private val isInitialized get() = ::client.isInitialized
    var config = "webhook.conf"

    override fun start() {
        super.start()
        try {
            val config = loadConfig(config)
            val webhook = config.config("webhook") ?: return addError("Could not find configuration node: 'webhook'")
            val id = webhook.long("id")            ?: return addError("Could not find configuration node: 'webhook.id'")
            val token = webhook.string("token")    ?: return addError("Could not find configuration node: 'webhook.token'")
            this.client = WebhookClientBuilder(id, token).apply {
                this.setDaemon(true)
                this.setHttpClientBuilder(Laxus.HttpClientBuilder)
            }.build()
        } catch(t: Throwable) {
            addError("Could not start WebhookAppender due to an unexpected error: ", t)
        }
    }

    override fun append(event: ILoggingEvent) {
        if(!isInitialized) return
        try {
            client.send {
                title { event.loggerName.split('.').run { this[lastIndex] } }
                append(event.formattedMessage)
                val proxy = event.throwableProxy
                if(proxy !== null) {
                    appendln()
                    appendln()
                    val stackTrace = buildString {
                        appendln("```java")
                        append(proxy.className)
                        val message = proxy.message

                        if(message !== null) {
                            append(": $message")
                        }
                        appendln()
                        val arr = proxy.stackTraceElementProxyArray
                        for((index, element) in arr.withIndex()) {
                            val str = element.steAsString
                            if(str.length + this.length > EmbedLimit) {
                                append("\t... (${arr.size - index + 1} more calls)")
                                break
                            }
                            append("\t$str")
                            appendln()
                        }
                        append("```")
                    }
                    append(stackTrace)
                }
                color {
                    when(event.level) {
                        Level.INFO  -> Color.BLUE
                        Level.WARN  -> Color.ORANGE
                        Level.ERROR -> Color.RED
                        Level.DEBUG -> Color.YELLOW
                        else        -> null
                    }
                }
                footer { value = "Logged at" }
                time {
                    Calendar.getInstance(TimeZone.getTimeZone("GMT")).let { gmt ->
                        gmt.timeInMillis = event.timeStamp
                        OffsetDateTime.ofInstant(gmt.toInstant(), gmt.timeZone.toZoneId())
                    }
                }
            }
        } catch(t: Throwable) {
            addError("Caught an unhandled error when sending a message through WebhookClient: ", t)
        }
    }

    override fun stop() {
        super.stop()
        if(!isInitialized) return

        this.client.close()
    }
}