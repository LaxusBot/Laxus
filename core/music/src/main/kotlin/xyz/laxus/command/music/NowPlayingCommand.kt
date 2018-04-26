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
package xyz.laxus.command.music

import net.dv8tion.jda.core.Permission.*
import xyz.laxus.Laxus
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.menus.updatingMenu
import xyz.laxus.jda.menus.updatingMenuBuilder
import xyz.laxus.music.lava.trackEmbed
import xyz.laxus.util.concurrent.duration
import java.util.concurrent.TimeUnit

/**
 * @author Kaidan Gustave
 */
class NowPlayingCommand: MusicCommand(MusicGroup.Manager) {
    override val name = "NowPlaying"
    override val aliases = arrayOf("NP")
    override val help = "Gets what song is currently playing."
    override val cooldown = 20
    override val botPermissions = arrayOf(
        MESSAGE_MANAGE,
        MESSAGE_EMBED_LINKS,
        MESSAGE_ADD_REACTION
    )

    private val builder = updatingMenuBuilder {
        waiter { Laxus.Waiter }
        interval { duration(5, TimeUnit.SECONDS) }
        timeout {
            delay { 2 }
            unit { TimeUnit.MINUTES }
        }
    }

    override suspend fun execute(ctx: CommandContext) {
        val member = ctx.member
        val guild = ctx.guild
        if(!ctx.guild.isPlaying) return ctx.notPlaying()
        if(!member.inPlayingChannel) return ctx.notInPlayingChannel()
        val queue = checkNotNull(manager[guild])
        ctx.invokeCooldown()
        val menu = updatingMenu(builder) {
            update {
                if(!queue.isDead) {
                    trackEmbed(guild, queue.currentTrack, queue.peek(), queue.paused)
                } else {
                    color { guild.selfMember.color }
                    title { "Nothing playing" }
                    append("There is no track currently playing!")
                    it.cancel()
                }
            }
            finalAction {
                ctx.linkMessage(it)
                if(it.guild.selfMember.hasPermission(it.textChannel, MESSAGE_MANAGE)) {
                    it.clearReactions().queue()
                }
            }
            user { ctx.author }
        }

        menu.displayIn(ctx.textChannel)
    }
}