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

import io.ktor.client.HttpClient
import xyz.laxus.bot.command.*
import xyz.laxus.bot.requests.youtube.YouTubeRequester
import xyz.laxus.bot.utils.jda.await

@AutoCooldown(AutoCooldown.Mode.AFTER)
@MustHaveArguments("Specify what to search YouTube for.")
class YouTubeCommand(apiKey: String?, httpClient: HttpClient): Command(StandardGroup) {
    override val name = "YouTube"
    override val aliases = arrayOf("YT")
    override val arguments = "[Search Query]"
    override val help = "Searches YouTube."
    override val guildOnly = false
    override val cooldown = 30
    override val cooldownScope = CooldownScope.USER

    private val requester = YouTubeRequester(httpClient, apiKey)

    override suspend fun execute(ctx: CommandContext) {
        ctx.channel.sendTyping().await()
        val query = ctx.args
        val result = runCatching { requester.search(query) }.getOrCommandError {
            "An unexpected error occurred while searching!"
        }
        val items = result.items.mapNotNull { it.id?.videoId }
        commandErrorIf(items.isEmpty()) { "No results were found for '**$query**'!" }
        ctx.replySuccess("${ctx.author.asMention} https://youtube.com/watch?v=${items[0]}")
    }
}
