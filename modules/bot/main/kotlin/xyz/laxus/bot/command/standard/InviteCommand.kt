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

import net.dv8tion.jda.core.entities.User
import xyz.laxus.bot.Laxus
import xyz.laxus.bot.command.Command
import xyz.laxus.bot.command.CommandContext
import xyz.laxus.bot.command.commandError
import xyz.laxus.bot.utils.jda.await
import xyz.laxus.bot.utils.jda.boldFormattedName

/**
 * @author Kaidan Gustave
 */
class InviteCommand: Command(StandardGroup) {
    override val name = "Invite"
    override val help = "Gets an invite link for Laxus."
    override val guildOnly = false
    override val hasAdjustableLevel = false

    private lateinit var oAuth2Link: String

    override suspend fun execute(ctx: CommandContext) {
        if(!::oAuth2Link.isInitialized) {
            try {
                val info = ctx.jda.asBot().applicationInfo.await()
                oAuth2Link = info.getInviteUrl(*Laxus.Permissions)
            } catch(t: Throwable) {
                Laxus.Log.warn("Failed to generate OAuth2 URL: ${t.message}")
                commandError("An unexpected error occurred!")
            }
        }

        ctx.reply(buildString {
            appendln("Laxus is a general discord bot for moderation, utility, and larger communities!")
            appendln("To add me to your server, click the link below:")
            appendln()
            appendln("${Laxus.Success} **<$oAuth2Link>**")
            appendln()
            appendln("To see a full list of my commands, type `${ctx.bot.prefix}help`.")
            append("If you require additional help ")
            val owner = runCatching<User?> { ctx.jda.retrieveUserById(Laxus.DevId).await() }.getOrNull()
            if(owner !== null) {
                append("contact ${owner.boldFormattedName} or ")
            }
            append("join my support server **<${Laxus.ServerInvite}>**")
        })
    }
}
