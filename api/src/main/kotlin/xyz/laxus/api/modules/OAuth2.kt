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
package xyz.laxus.api.modules

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.config.ApplicationConfig
import io.ktor.features.origin
import io.ktor.http.Cookie
import io.ktor.http.CookieEncoding
import xyz.laxus.api.oauth2.ClientInfo
import xyz.laxus.api.oauth2.DashboardAuthenticator
import xyz.laxus.api.oauth2.OAuth2Session
import java.time.OffsetDateTime

object OAuth2: Module {
    private lateinit var discord: DashboardAuthenticator

    override fun Application.install() {
        val config = environment.config.config("ktor.auth")
        discord = DashboardAuthenticator(client(config.config("discord")))
        install(Authentication) {
            register(discord)
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

    private fun client(config: ApplicationConfig): ClientInfo = ClientInfo(
        name = config.property("name").getString(),
        id = config.property("client.id").getString(),
        secret = config.property("client.secret").getString()
    )

    private fun Authentication.Configuration.register(manager: DashboardAuthenticator) = manager.run { install() }
}
