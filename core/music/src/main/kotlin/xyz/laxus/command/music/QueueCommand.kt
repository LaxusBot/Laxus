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
import xyz.laxus.jda.menus.paginator
import xyz.laxus.jda.menus.paginatorBuilder
import xyz.laxus.util.displayTitle
import xyz.laxus.util.formattedInfo
import xyz.laxus.util.progression

/**
 * @author Kaidan Gustave
 */
class QueueCommand: MusicCommand(MusicGroup.manager) {
    override val name = "Queue"
    override val help = "Shows the currently queued music."
    override val botPermissions = arrayOf(
        MESSAGE_MANAGE,
        MESSAGE_EMBED_LINKS
    )

    private val builder = paginatorBuilder {
        waiter { Laxus.Waiter }
        timeout { delay { 20 } }
        itemsPerPage { 8 }
        showPageNumbers { true }
        numberItems { true }
        waitOnSinglePage { true }
    }

    override suspend fun execute(ctx: CommandContext) {
        val member = ctx.member
        if(!ctx.guild.isPlaying) return ctx.notPlaying()
        if(!member.inPlayingChannel) return ctx.notInPlayingChannel()

        val queue = checkNotNull(manager[ctx.guild]) {
            "Expected non-null guild queue for Guild (ID: ${ctx.guild.idLong})"
        }

        val tracks = queue.tracks

        if(tracks.isEmpty()) {
            return ctx.reply("Queue is empty!")
        }

        builder.clearItems()

        val paginator = paginator(builder) {
            allowTextInput = true
            textToRight = ">>"
            textToLeft = "<<"
            bulkSkipNumber = 5
            text { _, _ ->
                val q = manager[ctx.guild] ?: return@text "Now Playing: Nothing"
                return@text buildString {
                    append("Now Playing: ")
                    val current = q.currentTrack
                    if(q.paused) {
                        append("`(PAUSED)` ")
                    }
                    append(current.info.displayTitle)
                    append(" `[${current.progression}]`")
                }
            }
            items { + tracks.map { t -> t.info.formattedInfo } }
            finalAction { message -> message.delete().queue({}, {}) }
            user { ctx.author }
        }

        paginator.displayIn(ctx.textChannel)
    }
}