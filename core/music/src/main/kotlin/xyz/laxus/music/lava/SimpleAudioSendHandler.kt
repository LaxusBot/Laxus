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
package xyz.laxus.music.lava

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler

/**
 * Simple concrete implementation of [AudioSendHandler].
 *
 * @author Kaidan Gustave
 */
class SimpleAudioSendHandler(private val player: AudioPlayer): AudioSendHandler {
    private lateinit var lastFrame: AudioFrame

    override fun isOpus(): Boolean = true
    override fun provide20MsAudio(): ByteArray = lastFrame.data
    override fun canProvide(): Boolean {
        lastFrame = player.provide() ?: return false
        return true
    }
}