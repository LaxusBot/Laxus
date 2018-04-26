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

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.VoiceChannel
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.util.connectedChannel
import xyz.laxus.jda.util.editMessage
import xyz.laxus.music.MusicManager
import xyz.laxus.util.formattedInfo
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
abstract class MusicCommand(protected val manager: MusicManager): Command(MusicGroup) {
    private companion object {
        private const val YT_SEARCH_PREFIX = "ytsearch:"
    }

    protected val Guild.isPlaying get() = this in this@MusicCommand.manager
    protected val Member.inPlayingChannel: Boolean get() {
        val chan = connectedChannel ?: return false
        return chan == guild.selfMember.connectedChannel
    }

    protected val CommandContext.voiceChannel: VoiceChannel? get() {
        return member.connectedChannel
    }

    protected fun CommandContext.notPlaying(): Unit = replyError {
        "I must be playing music to use that command!"
    }

    protected fun CommandContext.notInVoiceChannel(): Unit = replyError {
        "You must be in a VoiceChannel to use music commands!"
    }

    protected fun CommandContext.notInPlayingChannel(): Unit = replyError {
        "You must be in ${selfMember.connectedChannel?.name} to use music commands!"
    }

    protected fun CommandContext.singleTrackLoaded(loading: Message, item: AudioTrack) {
        val voiceChannel = checkNotNull(voiceChannel) {
            "When rendering response for a loaded track, the voice channel to play in was null!"
        }
        item.userData = member
        val info = item.info.formattedInfo
        val position = manager.addTrack(voiceChannel, item)
        loading.editMessage {
            append(Laxus.Success)
            if(position < 1) {
                append(" Now playing $info.")
            } else {
                append(" Added $info at position $position in the queue.")
            }
        }.queue()
    }

    protected fun CommandContext.playlistLoaded(loading: Message, item: AudioPlaylist) {
        val voiceChannel = checkNotNull(voiceChannel) {
            "When rendering response for a loaded playlist, the voice channel to play in was null!"
        }

        val tracks = item.tracks.onEach { it.userData = member }
        manager.addTracks(voiceChannel, tracks)
        loading.editMessage {
            append(Laxus.Success)
            if(manager[guild]!!.size + 1 == tracks.size) {
                append(" Now playing `${tracks.size}` tracks from playlist **${item.name}**.")
            } else {
                append(" Added `${tracks.size}` tracks from **${item.name}**.")
            }
        }.queue()
    }

    protected fun unsupportedItemType(loading: Message) {
        loading.editMessage("The loaded item is unsupported by this player.").queue()
    }

    protected suspend fun loadTrack(
        member: Member, query: String,
        isSearchList: Boolean = false
    ): AudioItem? = suspendCoroutine { cont ->
        val handle = SearchHandler(cont, member, query, isSearchList, query.startsWith(YT_SEARCH_PREFIX))
        manager.loadItemOrdered(member.guild, query, handle)
    }

    protected inner class SearchHandler(
        private val continuation: Continuation<AudioItem?>,
        private val member: Member,
        private val query: String,
        private val isSearchList: Boolean,
        private var ytSearch: Boolean = false
    ): AudioLoadResultHandler {
        private val guild: Guild get() = member.guild

        override fun trackLoaded(track: AudioTrack) {
            continuation.resume(track)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            val tracks = playlist.tracks
            val selectedTrack = playlist.selectedTrack
            if(tracks.size == 1 || (playlist.isSearchResult && !isSearchList) || selectedTrack !== null) {
                trackLoaded(selectedTrack ?: tracks[0])
            } else {
                continuation.resume(playlist)
            }
        }

        override fun noMatches() {
            if(ytSearch) {
                continuation.resume(null)
            } else {
                rerun("ytsearch:$query")
            }
        }

        override fun loadFailed(exception: FriendlyException) {
            continuation.resumeWithException(exception)
        }

        private fun rerun(newQuery: String = query) {
            manager.loadItemOrdered(guild, newQuery, apply { ytSearch = true })
        }
    }

}