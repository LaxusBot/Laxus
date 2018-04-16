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

import net.dv8tion.jda.core.Permission
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.embed
import xyz.laxus.jda.util.message
import xyz.laxus.requests.GoogleImageAPI
import xyz.laxus.util.commandArgs

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify what to search for.")
class ImageCommand: Command(StandardGroup) {
    override val name = "Image"
    override val aliases = arrayOf("Img")
    override val arguments = "[Search Query]"
    override val help = "Searches for an image."
    override val guildOnly = false
    override val cooldown = 30
    override val cooldownScope = CooldownScope.USER
    override val botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)

    private val api = GoogleImageAPI()

    override suspend fun execute(ctx: CommandContext) {
        val query = ctx.args
        ctx.channel.sendTyping().await()
        val results = api.search(query)
        when {
            results === null  -> ctx.replyError("An unexpected error occurred while searching!")
            results.isEmpty() -> ctx.replyError("No results were found for \"**$query**\"!")
            else -> ctx.reply(message {
                append { "${Laxus.Success} ${ctx.author.asMention}" }
                embed {
                    if(ctx.isGuild) {
                        color { ctx.member.color }
                    }
                    image { selectResultURL(query, results) }
                }
            })
        }
        ctx.invokeCooldown()
    }

    private fun selectResultURL(query: String, results: List<String>): String {
        // Start with last index of the results
        var index = results.size - 1

        // Subtract the length of the query
        index -= query.length

        // If the index has fallen below or is at 0 return the first result
        if(index <= 0)
            return results[0]

        // If there is more than 2 spaces, divide the results by the number of them
        val spaces = query.split(commandArgs).size
        if(spaces > 2)
            index /= spaces - 1

        // Once again, grab first index if it's below or at 0
        if(index <= 0)
            return results[0]

        // return a random result between first index and the calculated maximum
        return results[(Math.random() * (index)).toInt()]
    }
}
