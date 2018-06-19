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
@file:Suppress("CanBeParameter", "MemberVisibilityCanBePrivate")
package xyz.laxus.api.oauth2

import io.ktor.auth.Authentication
import io.ktor.auth.OAuthServerSettings
import io.ktor.auth.oauth
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.http.HttpMethod

class DashboardAuthenticator(val client: ClientInfo) {
    val configuration = client.name
    val defaultScopes = listOf("bot", "identify", "guilds", "guilds.join")
    private val settings = OAuthServerSettings.OAuth2ServerSettings(
        name = client.name,
        clientId = client.id,
        clientSecret = client.secret,
        defaultScopes = defaultScopes,
        requestMethod = HttpMethod.Post,
        authorizeUrl = "https://discordapp.com/api/oauth2/authorize",
        accessTokenUrl = "https://discordapp.com/api/oauth2/token"
    )

    fun Authentication.Configuration.install() {
        oauth(configuration) {
            client = HttpClient(Apache)
            providerLookup = { settings }
            urlProvider = { "/dashboard/auth" }
        }
    }
}