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
package xyz.laxus.music.experimental.coroutines

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.*
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason.*
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.newSingleThreadContext
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import xyz.laxus.music.IMusicManager
import xyz.laxus.music.lava.member
import xyz.laxus.util.createLogger
import xyz.laxus.util.error
import xyz.laxus.util.formatTrackTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors.newCachedThreadPool
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Kaidan Gustave
 */
class CoroutineMusicManager : IMusicManager, AudioPlayerManager by DefaultAudioPlayerManager() {
    internal companion object {
        internal val LOG = createLogger(CoroutineMusicManager::class)

        internal fun logTrackInfo(track: AudioTrack): String {
            return "Title: ${track.info.title} | Length: ${formatTrackTime(track.duration)} | State: ${track.state}"
        }
    }

    private val queueMap = ConcurrentHashMap<Long, CoroutineMusicQueue>()

    internal val playContext by lazy(newCachedThreadPool(Factory())::asCoroutineDispatcher)
    internal val closeContext by lazy { newSingleThreadContext("AudioClose Context") }

    init {
        registerSourceManager(YoutubeAudioSourceManager())
    }

    override operator fun get(guild: Guild): CoroutineMusicQueue? = queueMap[guild.idLong]
    override operator fun contains(guild: Guild): Boolean = guild.idLong in queueMap

    override fun stop(guild: Guild) {
        val musicQueue = queueMap[guild.idLong] ?: return
        musicQueue.close()
        queueMap -= musicQueue.channel.guild.idLong
    }

    override fun addTrack(channel: VoiceChannel, track: AudioTrack): Int {
        val guild = channel.guild
        if(guild !in this) {
            setupPlayer(channel, track)
            return 0
        } else {
            val queue = queueMap[guild.idLong] ?: return -1
            return queue.queue(track)
        }
    }

    override fun addTracks(channel: VoiceChannel, tracks: List<AudioTrack>) {
        require(tracks.isNotEmpty()) { "Tracks is empty!" }

        val guild = channel.guild
        if(guild !in this) {
            setupPlayer(channel, tracks[0])
        }

        val queue = this[guild] ?: return LOG.error {
            "Attempted to get MusicQueue for Guild (ID: ${guild.idLong}) after checkpoint, but got null?!"
        }

        for(i in 1 until tracks.size) {
            queue.queue(tracks[i])
        }
    }

    override fun onEvent(event: Event) {
        when(event) {
            // Dispose on shutdown
            is ShutdownEvent -> {
                queueMap.forEach { _, u -> u.close() }
                closeContext.close()
                shutdown()
            }

            // Dispose if we leave a guild for whatever reason
            is GuildLeaveEvent -> removeGuild(event.guild.idLong)

            // Dispose if certain events are fired
            is GuildVoiceLeaveEvent -> if(event.isSelf) removeGuild(event.guild.idLong)
        }
    }

    override fun onEvent(event: AudioEvent) {
        when(event) {
            is TrackStartEvent -> {
                LOG.debug("Track Started | Title: ${event.track.info.title}")
            }

            is TrackEndEvent -> {
                when(event.endReason) {
                    null        -> return
                    FINISHED    -> onTrackFinished(event)
                    LOAD_FAILED -> LOG.debug("Track Load Failed | ${logTrackInfo(event.track)}")
                    STOPPED     -> LOG.debug("Track Stopped | ${logTrackInfo(event.track)}")
                    REPLACED    -> LOG.debug("Track Replaced | ${logTrackInfo(event.track)}")
                    CLEANUP     -> LOG.debug("Track Cleanup | ${logTrackInfo(event.track)}")
                }
            }

            is TrackExceptionEvent -> {
                LOG.error("Track Exception | ${logTrackInfo(event.track)}", event.exception)
            }

            is TrackStuckEvent -> {
                LOG.debug("Track Stuck | ${logTrackInfo(event.track)} | ${event.thresholdMs}ms")
            }
        }
    }

    internal fun newPlayer(): AudioPlayer = createPlayer().also { it.addListener(this) }

    private fun setupPlayer(voiceChannel: VoiceChannel, firstTrack: AudioTrack) {
        require(voiceChannel.guild !in this) {
            "Attempted to join a VoiceChannel on a Guild already being handled!"
        }
        queueMap[voiceChannel.guild.idLong] = CoroutineMusicQueue(this, voiceChannel, firstTrack)
    }

    private fun removeGuild(guildId: Long) {
        if(guildId in queueMap) {
            queueMap.remove(guildId)?.close()
        }
    }

    private fun onTrackFinished(event: TrackEndEvent) {
        LOG.debug("Track Finished | ${logTrackInfo(event.track)}")

        val member = event.track.member
        val guildQueue = queueMap[member.guild.idLong] ?: return

        guildQueue.awaken()

        if(guildQueue.isDead) {
            queueMap -= guildQueue.channel.guild.idLong
        }
    }

    private val GenericGuildVoiceEvent.isSelf get() = member == guild.selfMember

    private class Factory : ThreadFactory {
        private val threadNumber = AtomicInteger(0)
        private val threadName get() = "MusicManager - Thread ${threadNumber.getAndIncrement()}"

        override fun newThread(r: Runnable): Thread = Thread(r, threadName).also { it.isDaemon = true }
    }
}
