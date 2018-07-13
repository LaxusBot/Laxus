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

import xyz.laxus.command.CommandContext
import xyz.laxus.music.lava.member
import xyz.laxus.util.displayTitle
import xyz.laxus.util.formattedName

/**
 * @author Kaidan Gustave
 */
class SkipCommand: MusicCommand(MusicGroup.manager) {
    override val name = "Skip"
    override val help = "Votes to skip the currently playing song."

    override suspend fun execute(ctx: CommandContext) {
        val member = ctx.member
        if(!ctx.guild.isPlaying) return ctx.notPlaying()
        if(!member.inPlayingChannel) return ctx.notInPlayingChannel()

        val queue = checkNotNull(manager[ctx.guild]) {
            "Expected non-null guild queue for Guild (ID: ${ctx.guild.idLong})"
        }

        if(member == queue.currentTrack.member) {
            val skipped = queue.skip()
            return ctx.replySuccess("Skipped ${skipped.info.displayTitle}")
        }

        val totalToSkip = queue.totalToSkip
        if(totalToSkip == 1) {
            val skipped = queue.skip()
            return ctx.replySuccess {
                "Skipped ${skipped.info.displayTitle} (Queued by: ${skipped.member.user.formattedName(true)})"
            }
        }

        if(queue.isSkipping(member)) {
            return ctx.replyWarning("You have already voted to skip this song!")
        }

        val skips = queue.voteToSkip(member)

        if(totalToSkip == skips) {
            val skipped = queue.skip()
            ctx.replySuccess {
                "Skipped ${skipped.info.displayTitle} (Queued by: ${skipped.member.user.formattedName(true)})"
            }
        } else {
            ctx.replySuccess {
                "Voted to skip ${queue.currentTrack.info.displayTitle} " +
                "(`$skips/$totalToSkip` votes, `${totalToSkip - skips}` more needed to skip)"
            }
        }
    }
}