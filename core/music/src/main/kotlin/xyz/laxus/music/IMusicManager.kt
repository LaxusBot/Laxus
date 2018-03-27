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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener

/**
 * @author Kaidan Gustave
 */
interface IMusicManager<M, Q> : AudioEventListener, EventListener, AudioPlayerManager
    where M: IMusicManager<M, Q>, Q: IMusicQueue<Q, M> {
    operator fun get(guild: Guild): Q?
    operator fun contains(guild: Guild): Boolean

    fun stop(guild: Guild)

    fun addTrack(channel: VoiceChannel, track: AudioTrack): Int
    fun addTracks(channel: VoiceChannel, tracks: List<AudioTrack>)

    override fun onEvent(event: Event)
    override fun onEvent(event: AudioEvent)
}