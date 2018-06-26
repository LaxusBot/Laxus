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

import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.response.HttpResponse
import io.ktor.client.response.readBytes
import io.ktor.http.*
import me.kgustave.json.JSObject
import me.kgustave.json.readJSArray
import me.kgustave.json.readJSObject
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import xyz.laxus.Laxus
import xyz.laxus.jda.util.await

/**
 * @author Kaidan Gustave
 */
class DiscordOAuth2(private val client: HttpClient) {
    private companion object {
        private const val RestVersion = 6
        private const val BaseApiUrl = "https://discordapp.com/api/v$RestVersion"
        private const val CurrentUser = "/users/@me"
        private const val CurrentUserGuilds = "$CurrentUser/guilds"
        private val contentType = ContentType.Application.Json.withCharset(Charsets.UTF_8)
    }

    suspend fun user(session: OAuth2Session): User {
        val response = client.get<HttpResponse>(BaseApiUrl + CurrentUser) {
            contentType(contentType)
            accept(contentType.withoutParameters())
            headers.append(HttpHeaders.Authorization, authorizationHeader(session))
        }
        check(response.status.isSuccess()) { "Unsuccessful response from $CurrentUser" }
        val json = response.readBytes().readJSObject(response.charset() ?: Charsets.UTF_8)
        return Laxus.JDA.retrieveUserById(json.long("id")).await()
    }

    suspend fun guilds(session: OAuth2Session): List<Guild> {
        val response = client.get<HttpResponse>(BaseApiUrl + CurrentUserGuilds) {
            contentType(contentType)
            accept(contentType.withoutParameters())
            headers.append(HttpHeaders.Authorization, authorizationHeader(session))
        }
        check(response.status.isSuccess()) { "Unsuccessful response from $CurrentUserGuilds" }
        val json = response.readBytes().readJSArray(response.charset() ?: Charsets.UTF_8)
        return json.asSequence()
            .mapNotNull { (it as? JSObject)?.optLong("id") }
            .mapNotNull { Laxus.JDA.getGuildById(it) }
            .toList()
    }

    private fun authorizationHeader(session: OAuth2Session) = "${session.tokenType} ${session.accessToken}"

}