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

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.experimental.async
import net.dv8tion.jda.core.Permission
import xyz.laxus.command.CommandContext
import xyz.laxus.music.MusicManager
import xyz.laxus.util.*
import kotlin.coroutines.experimental.coroutineContext

/**
 * @author Kaidan Gustave
 */
class PlayCommand(manager: MusicManager): MusicCommand(manager) {
    override val name = "Play"
    override val arguments = "[Song|URL]"
    override val help = "Plays a song in your connected voice channel."
    override val botPermissions = arrayOf(
        Permission.VOICE_CONNECT,
        Permission.VOICE_SPEAK
    )

    override suspend fun execute(ctx: CommandContext) {
        val guild = ctx.guild
        val member = ctx.member
        val query = ctx.args

        if(query.isEmpty()) {
            // Allow this command to act as a stand-in for a separate "unpause" command
            val level = ctx.bot.commands["Pause"]!!.run { ctx.level }
            if(level.test(ctx)) {
                if(!guild.isPlaying) return ctx.notPlaying()
                if(!member.inPlayingChannel) return ctx.notInPlayingChannel()

                val queue = checkNotNull(manager[ctx.guild]) {
                    "Got a null MusicQueue after checking for playing!"
                }

                val currentTrack = queue.currentTrack
                val currentTrackInfo = currentTrack.info

                if(queue.paused) {
                    queue.paused = false
                    return ctx.replySuccess {
                        "Unpaused ${currentTrackInfo.displayTitle} at `[${currentTrack.progression}]`!"
                    }
                }
            }
            // Empty args for nothing
            return ctx.missingArgs { "Specify a song name, or URL link." }
        }

        val voiceChannel = ctx.voiceChannel

        if(!member.inPlayingChannel || voiceChannel === null) {
            if(guild.isPlaying) {
                return ctx.notInPlayingChannel()
            }
            if(voiceChannel === null) {
                return ctx.notInVoiceChannel()
            }
        }

        val loading = async(coroutineContext) {
            ctx.send("Loading...")
        }

        val item = try {
            loadTrack(member, query)
        } catch(e: FriendlyException) {
            val message = loading.await()
            return when(e.severity) {
                COMMON -> message.editMessage("An error occurred${e.message?.let { ": $it" } ?: ""}.").queue()
                else -> message.editMessage("An error occurred.").queue()
            }
        }

        when(item) {
            null -> ctx.replyWarning(noMatch("results", query))
            is AudioTrack -> ctx.singleTrackLoaded(loading, item)
            is AudioPlaylist -> ctx.playlistLoaded(loading, item)

            // This shouldn't happen, but...
            else -> unsupportedItemType(loading)
        }
    }
}