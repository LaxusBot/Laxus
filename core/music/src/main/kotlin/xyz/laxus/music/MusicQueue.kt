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
package xyz.laxus.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.experimental.launch
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.VoiceChannel
import xyz.laxus.jda.util.connect
import xyz.laxus.music.lava.SimpleAudioSendHandler
import xyz.laxus.music.lava.member
import xyz.laxus.util.collections.swap
import xyz.laxus.util.randomInt
import java.util.*

class MusicQueue internal constructor(
    override val manager: MusicManager,
    override val channel: VoiceChannel,
    override val player: AudioPlayer,
    private var track: AudioTrack
): AudioSendHandler by SimpleAudioSendHandler(player),
    List<AudioTrack>, Queue<AudioTrack>, IMusicQueue {
    private var dead = false
    private val skipping = HashSet<Long>()

    init {
        channel.connect(sender = this)
        player.startTrack(track, false)
    }

    override val tracks = LinkedList<AudioTrack>()
    
    override val isDead get() = dead
    override val currentTrack get() = track

    override val skips: Int get() {
        val currentIds = channel.members.mapNotNull { it.user.takeIf { !it.isBot }?.idLong }
        skipping.removeIf { it !in currentIds }
        return skipping.size
    }

    override fun queue(track: AudioTrack): Int {
        add(track)
        return indexOf(track) + 1
    }

    override fun shuffle(userId: Long): Int {
        val indexList = ArrayList<Int>(size)
        for(i in indices) {
            val member = this[i].member
            if(member.user.idLong == userId) {
                indexList += i
            }
        }
        indexList.trimToSize()
        for(i in indexList) {
            val j = randomInt(max = indexList.size)
            val places = i to indexList[j]
            tracks swap places
        }

        return indexList.size
    }

    override fun isSkipping(member: Member): Boolean = member.user.idLong in skipping

    override fun voteToSkip(member: Member): Int {
        skipping.add(member.user.idLong)
        return skips
    }

    override fun skip(): AudioTrack {
        val skippedTrack = currentTrack
        player.stopTrack()
        return skippedTrack
    }

    // Queue Implementations

    override fun element(): AudioTrack = tracks.element()
    override fun peek(): AudioTrack? = tracks.peek()

    override fun add(element: AudioTrack): Boolean = tracks.add(element)
    override fun offer(e: AudioTrack?): Boolean = tracks.offer(e)

    override fun remove(): AudioTrack {
        return poll() ?: throw NoSuchElementException("Could not remove current track because the MusicQueue is empty")
    }
    override fun poll(): AudioTrack? {
        if(isNotEmpty()) {
            track = tracks.remove()
            player.startTrack(currentTrack, false)
            skipping.clear()
            return currentTrack
        } else clear()
        return null
    }

    fun removeAt(index: Int): AudioTrack = tracks.removeAt(index)
    operator fun set(index: Int, element: AudioTrack): AudioTrack = tracks.set(index, element)

    // List Implementations

    override fun get(index: Int): AudioTrack = tracks[index]
    override fun indexOf(element: AudioTrack): Int = tracks.indexOf(element)
    override fun lastIndexOf(element: AudioTrack): Int = tracks.lastIndexOf(element)
    override fun listIterator(): ListIterator<AudioTrack> = tracks.listIterator()
    override fun listIterator(index: Int): ListIterator<AudioTrack> = tracks.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<AudioTrack> = tracks.subList(fromIndex, toIndex)

    // MutableCollection Implementations

    override val size get() = tracks.size

    override fun contains(element: AudioTrack): Boolean = tracks.contains(element)
    override fun containsAll(elements: Collection<AudioTrack>): Boolean = tracks.containsAll(elements)
    override fun isEmpty(): Boolean = tracks.isEmpty()
    override fun iterator(): MutableIterator<AudioTrack> = tracks.iterator()
    override fun addAll(elements: Collection<AudioTrack>): Boolean = tracks.addAll(elements)
    override fun remove(element: AudioTrack): Boolean = tracks.remove(element)
    override fun removeAll(elements: Collection<AudioTrack>): Boolean = tracks.removeAll(elements)
    override fun retainAll(elements: Collection<AudioTrack>): Boolean = tracks.retainAll(elements)
    override fun clear() {
        tracks.clear()
        skipping.clear()
        close()
    }

    // AutoCloseable Implementation

    override fun close() {
        // JDA Audio Connections MUST be closed on a separate thread
        launch(manager.context) {
            guild.audioManager.closeAudioConnection()
            dead = true
        }
        player.destroy()
    }

    // Other Implementations

    override fun hashCode(): Int = channel.idLong.hashCode()

    override fun equals(other: Any?): Boolean {
        if(other !is IMusicQueue)
            return false

        return channel == other.channel
    }

    override fun toString(): String {
        return "MusicQueue(VC: ${channel.idLong}, Queued: $size, Now Playing: ${currentTrack.identifier})"
    }
}
