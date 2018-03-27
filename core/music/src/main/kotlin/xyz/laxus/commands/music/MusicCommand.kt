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
package xyz.laxus.commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import xyz.laxus.jda.util.connectedChannel
import xyz.laxus.music.MusicManager
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
abstract class MusicCommand(protected val manager: MusicManager) {
    private companion object {
        private const val YT_SEARCH_PREFIX = "ytsearch:"
    }

    val Guild.isPlaying: Boolean get() = this in this@MusicCommand.manager
    val Member.inPlayingChannel: Boolean get() {
        val chan = connectedChannel ?: return false
        val self = guild.selfMember
        return chan == self.connectedChannel
    }

    suspend fun loadTrack(member: Member, query: String): AudioItem? = suspendCoroutine { cont ->
        val handle = SearchHandler(cont, member, query, query.startsWith(YT_SEARCH_PREFIX))
        manager.loadItemOrdered(member.guild, query, handle)
    }

    protected inner class SearchHandler(
        private val continuation: Continuation<AudioItem?>,
        private val member: Member,
        private val query: String,
        private var ytSearch: Boolean = false
    ): AudioLoadResultHandler {
        private val guild: Guild get() = member.guild

        override fun trackLoaded(track: AudioTrack) {
            continuation.resume(track)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            val tracks = playlist.tracks
            val selectedTrack = playlist.selectedTrack
            if(tracks.size == 1 || playlist.isSearchResult || selectedTrack !== null) {
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