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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.laxus.music.experimental.coroutines

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState.FINISHED
import kotlinx.coroutines.experimental.channels.produce
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import xyz.laxus.jda.util.connect
import xyz.laxus.music.IMusicQueue
import xyz.laxus.music.lava.SimpleAudioSendHandler
import xyz.laxus.util.warn
import java.util.HashSet
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * @author Kaidan Gustave
 */
class CoroutineMusicQueue(
    override val manager: CoroutineMusicManager,
    override val channel: VoiceChannel,
    private var track: AudioTrack,
    override val player: AudioPlayer = manager.newPlayer()
): AudioSendHandler by SimpleAudioSendHandler(player),
    IMusicQueue<CoroutineMusicQueue, CoroutineMusicManager> {
    @Volatile private lateinit var suspension: Continuation<Unit>
    @Volatile private var dead = false

    private val queue = ConcurrentLinkedQueue<AudioTrack>().also { it.offer(track) }
    private val skipping = HashSet<Long>()
    private val receiver = produce<AudioTrack>(manager.playContext) {
        while(!dead) {
            val next = queue.poll() ?: break // We have null, player needs to close
            suspendCoroutine<Unit> { continuation ->
                // Right now, we hold until the track has finished
                suspension = continuation
                track = next
                player.playTrack(track)
            }

            // If this happens
            if(track.state != FINISHED) {
                CoroutineMusicManager.LOG.warn {
                    "Somehow a track failed to stop ${CoroutineMusicManager.logTrackInfo(track)}"
                }
                player.stopTrack()
            }

            skipping.clear()
        }
        this.close()
        manager.stop(guild)
    }

    init {
        channel.connect(sender = this)
        player.playTrack(track)
    }

    override val currentTrack: AudioTrack get() = track
    override val isDead: Boolean get() = dead || receiver.isClosedForReceive
    override val tracks: List<AudioTrack> get() = queue.toList()
    override val size: Int get() = queue.size

    override val skips: Int get() {
        val currentIds = listening.map { it.user.idLong }
        skipping.removeIf { it !in currentIds }
        return skipping.size
    }

    override var volume: Int
        get() = player.volume
        set(value) { player.volume = value }

    override fun queue(track: AudioTrack): Int {
        queue += track
        return queue.size
    }

    override fun isSkipping(member: Member): Boolean {
        return member.user.idLong in skipping
    }

    override fun voteToSkip(member: Member): Int {
        skipping += member.user.idLong
        return skips
    }

    override fun skip(): AudioTrack {
        val skippedTrack = track
        player.stopTrack()
        return skippedTrack
    }

    override fun close() {
        launch(manager.closeContext) { guild.audioManager.closeAudioConnection() }
        player.destroy()
        receiver.cancel()
        dead = true
    }

    override fun hashCode(): Int = channel.idLong.hashCode()

    override fun equals(other: Any?): Boolean {
        if(other !is IMusicQueue<*, *>)
            return false

        return channel == other.channel
    }

    override fun toString(): String {
        return "CoroutineMusicQueue(VC: ${channel.idLong}, Queued: $size, Now Playing: ${currentTrack.identifier})"
    }

    internal fun awaken() {
        if(!::suspension.isInitialized) return
        suspension.resume(Unit)
    }
}