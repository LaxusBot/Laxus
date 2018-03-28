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
@file:Suppress("LiftReturnOrAssignment")
package xyz.laxus.command.standard

import net.dv8tion.jda.core.JDAInfo
import net.dv8tion.jda.core.Permission
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.embed
import xyz.laxus.util.formattedName

/**
 * @author Kaidan Gustave
 */
class AboutCommand : Command(StandardGroup) {
    override val name = "About"
    override val help = "Gets info about the bot."
    override val guildOnly = false
    override val hasAdjustableLevel = false
    override val botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)

    private lateinit var oAuth2Link: String

    override suspend fun execute(ctx: CommandContext) {
        if(!::oAuth2Link.isInitialized || oAuth2Link.isEmpty()) {
            try {
                val info = ctx.jda.asBot().applicationInfo.await()
                oAuth2Link = info.getInviteUrl(*Laxus.Permissions)
            } catch(t: Throwable) {
                Laxus.Log.warn("Failed to generate OAuth2 URL!")
                oAuth2Link = ""
            }
        }

        val embed = embed {
            if(ctx.isGuild) color { ctx.selfMember.color }
            author {
                this.value = "All About ${ctx.selfUser.name}"
                this.icon = ctx.selfUser.avatarUrl
            }

            val dev = ctx.jda.retrieveUserById(Laxus.DevId).await()

            + "Hello, I am **${ctx.selfUser.name}**!\n"
            + "I am a discord bot with many functions from utility, to moderation, to fun commands!\n"
            + "I was written in Kotlin by ${dev.formattedName(true)} using the [JDA Library](${JDAInfo.GITHUB}) "
            + "(${JDAInfo.VERSION}).\n"
            + "I am at [Version ${Laxus.Version}](${Laxus.GitHub}). To see a full list of my commands, "
            + "type `${ctx.bot.prefix}help`, or if you require additional assistance, join my "
            + "[support server](${Laxus.ServerInvite})!\n"
            + "If you want to invite me to your server, click [here]($oAuth2Link) or use "
            + "`${ctx.bot.prefix}invite`!"

            val shard = ctx.jda.shardInfo

            thumbnail { ctx.selfUser.effectiveAvatarUrl }

            field {
                this.name = if(shard !== null) "This Shard" else "Users"
                this.inline = true

                + "${ctx.jda.users.size} Unique${if(shard !== null) " Users" else ""}\n"
                if(shard !== null) {
                    + "${ctx.jda.guilds.size} Servers"
                } else {
                    + "${ctx.jda.guilds.stream().mapToInt { it.members.size }.sum()} Total"
                }
            }
            field {
                this.name = if(shard !== null) "" else "Channels"
                this.inline = true

                + "${ctx.jda.textChannels.size} Text${if(shard !== null) " Channels" else ""}\n"
                + "${ctx.jda.voiceChannels.size} Voice${if(shard !== null) " Channels" else ""}"
            }
            field("Stats", true) {
                if(shard !== null) {
                    + "${ctx.bot.totalGuilds} Servers\n"
                    + "${ctx.bot.messageCacheSize} Cached Messages\n"
                    + "Shard ${shard.shardId + 1}"
                } else {
                    + "${ctx.jda.guilds.size} Servers\n"
                    + "${ctx.bot.messageCacheSize} Cached Messages"
                }
            }

            footer {
                this.value = "Last Restart"
                this.icon = null
            }
            time { ctx.bot.startTime }
        }

        ctx.reply(embed)
    }
}
