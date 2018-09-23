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
@file:JvmName("VoiceUtil")
@file:Suppress("Unused")
package xyz.laxus.bot.utils.jda

import net.dv8tion.jda.core.audio.AudioReceiveHandler
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.managers.AudioManager

fun VoiceChannel.connect(
    sender: AudioSendHandler? = null,
    receiver: AudioReceiveHandler? = null
) = with(guild.audioManager) {
    openAudioConnection(this@connect)
    sendingHandler = sender
    receivingHandler = receiver
}

inline var AudioManager.receivingHandler: AudioReceiveHandler?
    get() = receiveHandler
    set(value) = setReceivingHandler(value)
