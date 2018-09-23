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
@file:JvmName("JDABuilderUtil__Inlined")
@file:Suppress("Unused")
package xyz.laxus.bot.utils.jda

import com.neovisionaries.ws.client.WebSocketFactory
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.IEventManager
import net.dv8tion.jda.core.utils.SessionController
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentMap
import net.dv8tion.jda.core.JDABuilder as Builder

@DslMarker
@Retention(AnnotationRetention.SOURCE)
annotation class JDABuilderDsl

@JDABuilderDsl
inline fun jda(
    accountType: AccountType,
    init: Builder.() -> Unit
): JDA = Builder(accountType).apply(init).build()

@JDABuilderDsl
inline fun Builder.token(lazy: () -> String) = apply { setToken(lazy()) }

@JDABuilderDsl
inline fun Builder.game(lazy: () -> String) = apply { setGame(playing(lazy())) }

@JDABuilderDsl
inline fun Builder.listening(lazy: () -> String) = apply { setGame(listeningTo(lazy())) }

@JDABuilderDsl
inline fun Builder.watching(lazy: () -> String) = apply { setGame(watching(lazy())) }

@JDABuilderDsl
inline fun Builder.streaming(url: String, lazy: () -> String) = apply { setGame(streaming(lazy(), url)) }

@JDABuilderDsl
inline fun Builder.status(lazy: () -> OnlineStatus) = apply { setStatus(lazy()) }

@JDABuilderDsl
inline fun Builder.manager(lazy: () -> IEventManager) = apply { setEventManager(lazy()) }

@JDABuilderDsl
inline fun Builder.listener(lazy: () -> Any) = apply { addEventListener(lazy()) }

@JDABuilderDsl
inline fun Builder.audioSendFactory(lazy: () -> IAudioSendFactory) = apply { setAudioSendFactory(lazy()) }

@JDABuilderDsl
inline fun Builder.idle(lazy: () -> Boolean) = apply { setIdle(lazy()) }

@JDABuilderDsl
inline fun Builder.shutdownHook(lazy: () -> Boolean) = apply { setEnableShutdownHook(lazy()) }

@JDABuilderDsl
inline fun Builder.audio(lazy: () -> Boolean) = apply { setAudioEnabled(lazy()) }

@JDABuilderDsl
inline fun Builder.autoReconnect(lazy: () -> Boolean) = apply { setAutoReconnect(lazy()) }

@JDABuilderDsl
inline fun Builder.contextMap(lazy: () -> ConcurrentMap<String, String>?) = apply { setContextMap(lazy()) }

@JDABuilderDsl
inline fun Builder.sessionController(lazy: () -> SessionController) = apply { setSessionController(lazy()) }

@JDABuilderDsl
inline fun Builder.webSocketFactory(
    factory: WebSocketFactory = WebSocketFactory(),
    init: WebSocketFactory.() -> Unit
) = apply { setWebsocketFactory(factory.apply(init)) }

@JDABuilderDsl
inline fun Builder.httpSettings(
    builder: OkHttpClient.Builder = OkHttpClient.Builder(),
    init: OkHttpClient.Builder.() -> Unit = {}
) = apply { setHttpClientBuilder(builder.apply(init)) }

@JDABuilderDsl
inline fun <reified E: Event> Builder.on(
    single: Boolean = true,
    crossinline handle: (E) -> Unit
) = listener {
    object: EventListener {
        override fun onEvent(event: Event?) {
            if(event is E) {
                handle(event)
                if(single) {
                    event.jda.removeEventListener(this)
                }
            }
        }
    }
}
