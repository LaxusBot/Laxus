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
import io.ktor.auth.authentication
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.features.origin
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import me.kgustave.ktor.client.okhttp.OkHttp
import xyz.laxus.api.Module
import java.time.OffsetDateTime

object OAuth2: Module {
    private val httpClient = HttpClient(OkHttp)

    override fun Application.install() {
        val config = environment.config.config("ktor.auth")
        val client = ClientInfo.from(config.config("discord"))
        val settings = OAuth2Settings(
            name = client.name,
            clientId = client.id,
            clientSecret = client.secret,
            defaultScopes = client.defaultScopes,
            requestMethod = client.method,
            authorizeUrl = "https://discordapp.com/api/oauth2/authorize",
            accessTokenUrl = "https://discordapp.com/api/oauth2/token"
        )

        authentication {
            oauth(client.name) {
                this.client = httpClient
                this.providerLookup = { settings }
                this.urlProvider = { request.origin.uri }
            }
        }
    }

    fun cookies(session: OAuth2Session, domain: String): Set<Cookie> {
        // we expire the token 30 seconds early as a safe measure
        val expires = OffsetDateTime.now().plusSeconds(session.expiresIn - 30)
        val accessCookie = Cookie(
            name = "access_token",
            value = session.accessToken,
            encoding = CookieEncoding.BASE64_ENCODING,
            expires = expires,
            domain = domain,
            secure = true
        )
        val refreshCookie = Cookie(
            name = "refresh_token",
            value = session.refreshToken!!,
            encoding = CookieEncoding.BASE64_ENCODING,
            expires = expires,
            domain = domain,
            secure = true
        )
        return setOf(accessCookie, refreshCookie)
    }
}
