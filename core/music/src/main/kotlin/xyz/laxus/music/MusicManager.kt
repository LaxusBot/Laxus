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
@file:Suppress("unused")

package xyz.laxus.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.*
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.experimental.newSingleThreadContext
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import xyz.laxus.music.lava.ServiceAudioSourceManager
import xyz.laxus.music.lava.member
import xyz.laxus.util.createLogger
import xyz.laxus.util.formatTrackTime
import xyz.laxus.util.niceName

/**
 * @author Kaidan Gustave
 */
class MusicManager : IMusicManager, AutoCloseable, AudioPlayerManager by ServiceAudioSourceManager() {
    internal companion object {
        internal val Log = createLogger(MusicManager::class)

        private fun logTrackInfo(track: AudioTrack): String {
            return "Title: ${track.info.title} | Length: ${formatTrackTime(track.duration)} | State: ${track.state}"
        }
    }

    private val queueMap = hashMapOf<Long, MusicQueue>()
    internal val context by lazy { newSingleThreadContext("AudioClose Context") }

    override operator fun get(guild: Guild): MusicQueue? = synchronized(queueMap) { queueMap[guild.idLong] }
    override operator fun contains(guild: Guild): Boolean = synchronized(queueMap) { guild.idLong in queueMap }

    override fun stop(guild: Guild) {
        val musicQueue = this[guild] ?: return
        musicQueue.close()
        synchronized(queueMap) { queueMap -= guild.idLong }
    }

    override fun addTrack(channel: VoiceChannel, track: AudioTrack): Int {
        if(channel.guild !in this) {
            setupPlayer(channel, track)
            return 0
        } else {
            val queue = this[channel.guild] ?: return -1
            return queue.queue(track)
        }
    }

    override fun addTracks(channel: VoiceChannel, tracks: List<AudioTrack>) {
        if(channel.guild !in this) {
            setupPlayer(channel, tracks[0])
        }

        val queue = this[channel.guild] ?: return
        for(i in 1 until tracks.size) {
            queue.queue(tracks[i])
        }
    }

    override fun onEvent(event: Event) {
        when(event) {
            // Dispose on shutdown
            is ShutdownEvent -> close()

            // Dispose if we leave a guild for whatever reason
            is GuildLeaveEvent -> removeGuild(event.guild.idLong)

            // Dispose if certain events are fired
            is GuildVoiceLeaveEvent -> if(event.isSelf) removeGuild(event.guild.idLong)
        }
    }

    override fun onEvent(event: AudioEvent) {
        when(event) {
            is TrackStartEvent -> Log.debug("Track Started | ${logTrackInfo(event.track)}")
            is TrackEndEvent -> {
                val endReason = event.endReason
                if(endReason === null) {
                    Log.debug("Track Ended With Null Reason | ${logTrackInfo(event.track)}")
                } else {
                    Log.debug("Track ${endReason.niceName} | ${logTrackInfo(event.track)}")
                }
                onTrackFinished(event)
            }
            is TrackExceptionEvent -> Log.error("Track Exception | ${logTrackInfo(event.track)}", event.exception)
            is TrackStuckEvent -> {
                Log.debug("Track Stuck | ${logTrackInfo(event.track)} | ${event.thresholdMs}ms")
                event.player.stopTrack()
            }
        }
    }

    override fun close() {
        val queues = synchronized(queueMap) { queueMap.values }
        queues.forEach { it.close() }
        context.close()
        shutdown()
    }

    private fun setupPlayer(voiceChannel: VoiceChannel, firstTrack: AudioTrack) {
        require(voiceChannel.guild !in this) { "Attempted to join a VoiceChannel on a Guild already being handled!" }
        val player = createPlayer()
        player.addListener(this)
        synchronized(queueMap) {
            queueMap[voiceChannel.guild.idLong] = MusicQueue(this, voiceChannel, player, firstTrack)
        }
    }

    private fun removeGuild(guildId: Long) {
        synchronized(queueMap) {
            if(guildId in queueMap) {
                queueMap.remove(guildId)?.close()
            }
        }
    }

    private fun onTrackFinished(event: TrackEndEvent) {
        val member = event.track.member
        val guild = member.guild
        val guildQueue = this[guild] ?: return

        guildQueue.poll()
        if(guildQueue.isDead) {
            synchronized(queueMap) {
                queueMap -= guild.idLong
            }
        }
    }

    private val GenericGuildVoiceEvent.isSelf get() = member == guild.selfMember
}
