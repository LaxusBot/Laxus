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
import xyz.laxus.bot.utils.jda.await
import xyz.laxus.bot.requests.google.GoogleRequester

@AutoCooldown(AutoCooldown.Mode.AFTER)
@MustHaveArguments("Specify what to search Google for.")
class GoogleCommand(httpClient: HttpClient): Command(StandardGroup) {
    override val name = "Google"
    override val aliases = arrayOf("G")
    override val arguments = "[Search Query]"
    override val help = "Searches Google."
    override val guildOnly = false
    override val cooldown = 30
    override val cooldownScope = CooldownScope.USER

    private val api = GoogleRequester(httpClient)

    override suspend fun execute(ctx: CommandContext) {
        ctx.channel.sendTyping().await()
        val results = api.search(ctx.args)
        commandErrorIfNull(results) { "An unexpected error occurred while searching!" }
        commandErrorIf(results.isEmpty()) { "No results were found for '**${ctx.args}**'!" }
        ctx.replySuccess("${ctx.author.asMention} ${results[0]}")
    }
}
