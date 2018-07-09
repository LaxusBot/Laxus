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
@file:Suppress("FunctionName")

package xyz.laxus.api.routing

import io.ktor.application.ApplicationCall
import io.ktor.auth.authentication
import io.ktor.client.HttpClient
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import xyz.laxus.api.data.GuildInfo
import xyz.laxus.api.data.TagInfo
import xyz.laxus.api.data.UserInfo
import xyz.laxus.api.error.badRequest
import xyz.laxus.api.error.notFound
import xyz.laxus.api.handlers.annotations.*
import xyz.laxus.api.oauth2.DiscordOAuth2
import xyz.laxus.api.oauth2.OAuth2Session
import xyz.laxus.api.routing.locations.GuildsPath
import xyz.laxus.util.db.tags

@Authenticated("discord")
@RoutePath("/dashboard")
class Dashboard(httpClient: HttpClient) {
    private val discordOAuth = DiscordOAuth2(httpClient)

    @Get("/user") @Code(Code.OK)
    suspend fun `Get Dashboard User`(call: ApplicationCall): UserInfo {
        val session = call.sessions.get<OAuth2Session>() ?: call.authentication.principal() ?: incompleteFlow()
        val user = discordOAuth.user(session)
        return UserInfo(
            id = user.idLong,
            name = user.name,
            discriminator = user.discriminator.toInt(),
            avatar = user.effectiveAvatarUrl,
            tags = user.tags.map { TagInfo(it.name, it.content) }
        )
    }

    @Get("/guilds/{id}") @Code(Code.OK)
    suspend fun `Get Dashboard Guild`(call: ApplicationCall, @Locate path: GuildsPath): GuildInfo {
        val session = call.sessions.get<OAuth2Session>() ?: call.authentication.principal() ?: incompleteFlow()
        val guild = discordOAuth.guild(path.id, session) ?: throw notFound("Guild with ID '${path.id}' not found!")
        return GuildInfo(
            id = guild.idLong,
            name = guild.name,
            icon = guild.iconUrl,
            members = guild.memberCache.size(),
            tags = guild.tags.map { TagInfo(it.name, it.content, it.ownerId) }
        )
    }

    private fun incompleteFlow(): Nothing = throw badRequest("Could not complete OAuth2 flow!")
}