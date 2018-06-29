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
package xyz.laxus.api.oauth2

import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.AuthenticationPipeline
import io.ktor.auth.authentication
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.features.origin
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding.BASE64_ENCODING
import io.ktor.sessions.*
import me.kgustave.ktor.client.okhttp.OkHttp
import xyz.laxus.api.Module
import xyz.laxus.db.DBOAuth2Session
import xyz.laxus.db.entities.OAuth2Data
import java.time.OffsetDateTime

object OAuth2: Module {
    private val httpClient = HttpClient(OkHttp)

    private const val sessionCookieName = "discord_access_token"

    override fun Application.install() {
        val config = environment.config.config("ktor.auth")
        val info = ClientInfo.from(config.config("discord"))
        val settings = OAuth2Settings(
            name = info.name,
            clientId = info.id,
            clientSecret = info.secret,
            defaultScopes = info.defaultScopes,
            requestMethod = info.method,
            authorizeUrl = "https://discordapp.com/api/oauth2/authorize",
            accessTokenUrl = "https://discordapp.com/api/oauth2/token"
        )

        install(Sessions) {
            register(SessionProvider("oauth2", OAuth2Session::class, Transport(), Tracker()))
        }

        authentication {
            oauth(info.name) {
                client = httpClient
                providerLookup = { settings }
                urlProvider = { request.origin.run { "$scheme://$host:$port$uri" } }
                skipWhen { it.sessions.get<OAuth2Session>() !== null }
                pipeline.intercept(AuthenticationPipeline.RequestAuthentication) {
                    val principle = it.principal
                    if(principle is OAuth2Session) {
                        if(call.sessions.get<OAuth2Session>() === null) {
                            call.sessions.set("oauth2", principle)
                        }
                    }
                }
            }
        }
    }

    private fun ApplicationCall.discordAccessToken(): String? {
        return request.cookies[sessionCookieName, BASE64_ENCODING]
    }

    private class Tracker: SessionTracker {
        override suspend fun clear(call: ApplicationCall) {
            val accessToken = call.discordAccessToken() ?: return
            remove(accessToken)
        }

        override suspend fun load(call: ApplicationCall, transport: String?): Any? {
            val id = transport ?: call.discordAccessToken() ?: return null
            return retrieve(id)
        }

        override suspend fun store(call: ApplicationCall, value: Any): String {
            val session = (value as OAuth2Session)
            store(session)
            return session.accessToken
        }

        override fun validate(value: Any) {
            require(value is OAuth2Session) { "Cannot track session of type: ${value::class}" }
        }
    }

    private class Transport: SessionTransport {
        override fun clear(call: ApplicationCall) {
            call.response.cookies.appendExpired(sessionCookieName, call.request.origin.host)
        }

        override fun receive(call: ApplicationCall): String? {
            return call.discordAccessToken()
        }

        override fun send(call: ApplicationCall, value: String) {
            val session = retrieve(value) ?: return
            call.response.cookies.append(Cookie(
                name = sessionCookieName,
                value = value,
                encoding = BASE64_ENCODING,
                expires = OffsetDateTime.now().plusSeconds(session.expiresIn),
                domain = call.request.origin.host,
                secure = true
            ))
        }
    }

    // Private functions

    private fun retrieve(accessToken: String) = DBOAuth2Session.retrieve(accessToken)?.toSession()
    private fun remove(accessToken: String) = DBOAuth2Session.remove(accessToken)
    private fun store(session: OAuth2Session) = DBOAuth2Session.store(session.toData())

    private fun OAuth2Data.toSession() = OAuth2Session(accessToken, tokenType, expiration, refreshToken)
    private fun OAuth2Session.toData() = OAuth2Data(accessToken, refreshToken!!, tokenType, expiresIn)
}
