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
package xyz.laxus.bot.webhook

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.webhook.WebhookClient
import net.dv8tion.jda.webhook.WebhookClientBuilder
import xyz.laxus.bot.Laxus
import xyz.laxus.bot.utils.jda.embed
import xyz.laxus.config.loadConfig
import xyz.laxus.config.long
import xyz.laxus.config.string
import java.awt.Color
import java.time.OffsetDateTime
import java.util.*

class WebhookAppender: AppenderBase<ILoggingEvent>() {
    private companion object {
        private const val EmbedLimit = MessageEmbed.TEXT_MAX_LENGTH - 100
    }

    private lateinit var client: WebhookClient
    private val isInitialized get() = ::client.isInitialized
    var config = "webhook.conf"

    override fun start() {
        super.start()
        try {
            val config = loadConfig(config)
            val id = config.long("webhook.id")
            val token = config.string("webhook.token")
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
            val embed = embed {
                title { event.loggerName.split('.').run { this[lastIndex] } }
                append(event.formattedMessage)
                event.throwableProxy?.let { proxy ->
                    appendln()
                    appendln()
                    appendln("```java")
                    append(proxy.className)
                    proxy.message?.let { message -> append(": $message") }
                    appendln()
                    val array = proxy.stackTraceElementProxyArray
                    for((i, str) in array.asSequence().map { it.steAsString }.withIndex()) {
                        if(str.length + this.length > EmbedLimit) {
                            append("\t... (${array.size - i + 1} more calls)")
                            break
                        }
                        appendln("\t$str")
                    }
                    append("```")
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
                footer { value = "Logged" }
                time {
                    Calendar.getInstance(TimeZone.getTimeZone("GMT")).let { gmt ->
                        gmt.timeInMillis = event.timeStamp
                        OffsetDateTime.ofInstant(gmt.toInstant(), gmt.timeZone.toZoneId())
                    }
                }
            }

            client.send(embed)
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
