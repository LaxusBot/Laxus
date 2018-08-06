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
package xyz.laxus.command.standard

import io.ktor.client.request.get
import me.kgustave.json.JSArray
import me.kgustave.json.JSObject
import net.dv8tion.jda.core.Permission
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.embed
import java.time.OffsetDateTime
import java.time.OffsetDateTime.*

@MustHaveArguments("Specify a game to lookup info on.")
class GameCommand: Command(StandardGroup) {
    override val name = "Game"
    override val aliases = arrayOf("GameInfo")
    override val arguments = "[Game]"
    override val help = "Gets info on a game by name."
    override val guildOnly = false
    override val cooldown = 5
    override val cooldownScope = CooldownScope.USER
    override val hasAdjustableLevel = false
    override val botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)

    private val info = hashMapOf<String, GameInfo>()
    private lateinit var updatedAt: OffsetDateTime

    override suspend fun execute(ctx: CommandContext) {
        if(!::updatedAt.isInitialized || updatedAt.plusHours(updateEvery).isBefore(now())) {
            info.clear()
            ctx.bot.httpClient.get<JSArray>(requestUrl)
                .asSequence()
                .map { it as JSObject }
                .map { json -> createGameInfo(json) }
                .forEach { game ->
                    info[formatMapKeyName(game.name)] = game
                    for(name in game.aliases) {
                        info.computeIfAbsent(formatMapKeyName(name)) { game }
                    }
                }
        }

        val game = info[formatMapKeyName(ctx.args)] ?: return ctx.replyError {
            "Game matching \"${ctx.args}\" was not found!"
        }

        val embed = embed {
            title { game.name }
            color { ctx.takeIf { it.isGuild }?.selfMember?.color }
            thumbnail = game.icon

            + (game.summary ?: "No summary available for \"${game.name}\"!")

            if(game.developers.isNotEmpty()) {
                field(if(game.developers.size == 1) "Developer" else "Developers") {
                    + game.developers.joinToString()
                }
            }

            if(game.publishers.isNotEmpty()) {
                field(if(game.publishers.size == 1) "Publisher" else "Publishers") {
                    + game.publishers.joinToString()
                }
            }
        }

        ctx.reply(embed)
    }

    data class GameInfo(
        val id: Long,
        val name: String,
        val icon: String? = null,
        val summary: String? = null,
        val youtubeTrailer: String? = null,
        val aliases: List<String> = emptyList(),
        val developers: List<String> = emptyList(),
        val publishers: List<String> = emptyList()
    )

    private companion object {
        private const val updateEvery = 12L //hours

        private const val requestUrl = "https://discordapp.com/api/games"

        private fun formatMapKeyName(name: String) = name.replace(" ", "").toLowerCase()

        private fun createGameInfo(json: JSObject): GameInfo {
            val id = json.optLong("id") ?: 0L
            val icon = json.optString("icon")?.let { "https://cdn.discordapp.com/game-assets/$id/$it.png" }
            val youtubeTrailer = json.optString("youtube_trailer_id")?.let { "https://www.youtube.com/watch?v=$it" }
            return GameInfo(
                id = id,
                name = json.string("name"),
                icon = icon,
                summary = json.optString("summary"),
                youtubeTrailer = youtubeTrailer,
                aliases = json.optArray("aliases")?.mapNotNull { it as? String } ?: emptyList(),
                developers = json.optArray("aliases")?.mapNotNull { it as? String } ?: emptyList(),
                publishers = json.optArray("aliases")?.mapNotNull { it as? String } ?: emptyList()
            )
        }
    }
}
