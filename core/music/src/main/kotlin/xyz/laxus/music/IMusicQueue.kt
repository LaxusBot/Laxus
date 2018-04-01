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
package xyz.laxus.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import xyz.laxus.util.modifyIf

/**
 * @author Kaidan Gustave
 */
interface IMusicQueue: AudioSendHandler, AutoCloseable {
    val manager: IMusicManager
    val channel: VoiceChannel
    val currentTrack: AudioTrack
    val isDead: Boolean
    val tracks: List<AudioTrack>
    val skips: Int
    val player: AudioPlayer

    var volume: Int
        get() = player.volume
        set(value) { player.volume = value }
    var paused: Boolean
        get() = player.isPaused
        set(value) { player.isPaused = value }

    val size: Int get() = tracks.size

    val guild: Guild get() {
        return channel.guild
    }

    val listening: List<Member> get() {
        return channel.members.filter { !it.user.isBot }
    }

    val listeningCount: Int get() {
        return channel.members.count { !it.user.isBot }
    }

    val totalToSkip: Int get() {
        val totalMembers = listeningCount
        return (totalMembers / 2).modifyIf(totalMembers % 2 != 0) { it + 1 }
    }

    fun shuffle(userId: Long): Int
    fun queue(track: AudioTrack): Int
    fun isSkipping(member: Member): Boolean
    fun voteToSkip(member: Member): Int
    fun skip(): AudioTrack

    override fun isOpus(): Boolean = true
}