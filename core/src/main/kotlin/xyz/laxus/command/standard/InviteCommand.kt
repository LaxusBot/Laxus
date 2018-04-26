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

import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.util.await
import xyz.laxus.util.formattedName
import xyz.laxus.util.ignored

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
                return ctx.replyError("An unexpected error occurred!")
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
            val owner = ignored(null) { ctx.jda.retrieveUserById(Laxus.DevId).await() }
            if(owner !== null) {
                append("contact ${owner.formattedName(true)} or ")
            }
            append("join my support server **<${Laxus.ServerInvite}>**")
        })
    }
}
