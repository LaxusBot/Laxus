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
package xyz.laxus.jda.util

import com.neovisionaries.ws.client.WebSocketFactory
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder as Builder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.IEventManager
import net.dv8tion.jda.core.utils.SessionController
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentMap

inline fun jda(accountType: AccountType, init: Builder.() -> Unit): JDA {
    return Builder(accountType).apply(init).buildAsync()
}

inline fun Builder.token(lazy: () -> String): Builder = apply { setToken(lazy()) }
inline fun Builder.game(lazy: () -> String): Builder = apply { setGame(playing(lazy())) }
inline fun Builder.listening(lazy: () -> String): Builder = apply { setGame(listeningTo(lazy())) }
inline fun Builder.watching(lazy: () -> String): Builder = apply { setGame(watching(lazy())) }
inline fun Builder.streaming(url: String, lazy: () -> String): Builder = apply { setGame(streaming(lazy(), url)) }
inline fun Builder.status(lazy: () -> OnlineStatus): Builder = apply { setStatus(lazy()) }
inline fun Builder.manager(lazy: () -> IEventManager): Builder = apply { setEventManager(lazy()) }
inline fun Builder.listener(lazy: () -> Any): Builder = apply { addEventListener(lazy()) }
inline fun Builder.audioSendFactory(lazy: () -> IAudioSendFactory): Builder = apply { setAudioSendFactory(lazy()) }
inline fun Builder.idle(lazy: () -> Boolean): Builder = apply { setIdle(lazy()) }
inline fun Builder.shutdownHook(lazy: () -> Boolean): Builder = apply { setEnableShutdownHook(lazy()) }
inline fun Builder.audio(lazy: () -> Boolean): Builder = apply { setAudioEnabled(lazy()) }
inline fun Builder.autoReconnect(lazy: () -> Boolean): Builder = apply { setAutoReconnect(lazy()) }
inline fun Builder.contextMap(lazy: () -> ConcurrentMap<String, String>?): Builder = apply { setContextMap(lazy()) }
inline fun Builder.sessionController(lazy: () -> SessionController): Builder = apply { setSessionController(lazy()) }
inline fun Builder.webSocketFactory(
    factory: WebSocketFactory = WebSocketFactory(),
    init: WebSocketFactory.() -> Unit
): Builder = apply { setWebsocketFactory(factory.apply(init)) }
inline fun Builder.httpSettings(
    builder: OkHttpClient.Builder = OkHttpClient.Builder(),
    init: OkHttpClient.Builder.() -> Unit = {}
): Builder = apply { setHttpClientBuilder(builder.apply(init)) }
inline fun <reified E: Event> Builder.on(single: Boolean = true, crossinline handle: (E) -> Unit): Builder = listener {
    object : EventListener {
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
