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

import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.await
import xyz.laxus.requests.YouTubeAPI

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify what to search YouTube for.")
class YouTubeCommand(key: String?): Command(StandardGroup) {
    override val name = "YouTube"
    override val aliases = arrayOf("YT")
    override val arguments = "[Search Query]"
    override val help = "Searches YouTube."
    override val guildOnly = false
    override val cooldown = 30
    override val cooldownScope = CooldownScope.USER

    private val api = YouTubeAPI(key)

    override suspend fun execute(ctx: CommandContext) {
        val query = ctx.args
        ctx.channel.sendTyping().await()
        val results = api.search(query)
        when {
            results == null -> ctx.replyError("An unexpected error occurred while searching!")
            results.isEmpty() -> ctx.replyError("No results were found for \"**$query**\"!")
            else -> ctx.replySuccess("${ctx.author.asMention} https://youtube.com/watch?v=${results[0]}")
        }
        ctx.invokeCooldown()
    }

}