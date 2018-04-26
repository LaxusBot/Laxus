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

import net.dv8tion.jda.core.entities.Invite
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.embed
import xyz.laxus.util.readableFormat
import kotlin.text.RegexOption.*

/**
 * @author Kaidan Gustave
 */
class InviteInfoCommand: Command(StandardGroup) {
    private companion object {
        private const val Bullet = "\uD83D\uDD39"
        private val Invites = Regex("(?:https?//)?discord(?:\\.gg|app\\.com/invite)/([A-Z0-9-]{2,18})", IGNORE_CASE)
    }

    override val name = "InviteInfo"
    override val aliases = arrayOf("InvInfo")
    override val arguments = "[Invite|Invite Code]"
    override val help = "Gets info on a server via invite code."
    override val guildOnly = false

    override suspend fun execute(ctx: CommandContext) {
        val args = ctx.args
        val code = Invites.matchEntire(args)?.let { it.groupValues[1] } ?: args
        val invite = try {
            Invite.resolve(ctx.jda, code).await()
        } catch(e: ErrorResponseException) {
            return ctx.invalidArgs { "The invite/code you provided was invalid or cannot be looked up!" }
        }

        val guild = invite.guild
        val channel = invite.channel
        val embed = embed {
            title { "Invite to ${guild.name}" }
            url { invite.url }
            guild.iconUrl?.let { iconUrl -> thumbnail { iconUrl } }
            field(name = "Guild") {
                appendln("$Bullet **Guild Name:** ${guild.name}")
                appendln("$Bullet **Guild ID:** ${guild.idLong}")
                appendln("$Bullet **Guild Creation Date:** ${guild.creationTime.readableFormat}")
            }
            field(name = "Channel") {
                appendln("$Bullet **Channel Name:** #${channel.name}")
                appendln("$Bullet **Channel ID:** ${channel.idLong}")
                appendln("$Bullet **Channel Creation Date:** ${channel.creationTime.readableFormat}")
            }
        }

        ctx.reply(embed)
    }
}