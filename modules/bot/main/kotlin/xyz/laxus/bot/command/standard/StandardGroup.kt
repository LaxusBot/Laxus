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
package xyz.laxus.bot.command.standard

import com.typesafe.config.Config
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JsonFeature
import xyz.laxus.bot.command.CommandGroup
import xyz.laxus.bot.command.CommandLevel
import xyz.laxus.bot.requests.ktor.RequestSerializer
import xyz.laxus.config.nullString

object StandardGroup: CommandGroup("Standard") {
    override val defaultLevel = CommandLevel.PUBLIC
    override val guildOnly = false
    override val devOnly = false

    private val requesterClient = HttpClient(OkHttp) {
        install(JsonFeature) {
            serializer = RequestSerializer()
        }
    }

    override fun init(config: Config) {
        + AboutCommand()
        + AFKCommand()
        + AvatarCommand()
        + GoogleCommand(requesterClient)
        + ImageCommand(requesterClient)
        + InviteInfoCommand()
        + PingCommand()
        + YouTubeCommand(config.nullString("keys.youtube"), requesterClient)
    }
}
