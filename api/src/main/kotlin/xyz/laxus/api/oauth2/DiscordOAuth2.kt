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
import net.dv8tion.jda.core.entities.Guild

/**
 * @author Kaidan Gustave
 */
class DiscordOAuth2(private val client: HttpClient) {
    companion object {
        private const val RestVersion = 6
        private const val BaseApiUrl = "https://discordapp.com/api/v$RestVersion"
        private const val CurrentUser = "/users/@me"
        private const val CurrentUserGuilds = "$CurrentUser/guilds"
    }

    suspend fun guilds(session: OAuth2Session): List<Guild> {
        TODO("Implement")
    }
}