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

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import java.util.*
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager as Default

/**
 * [Default audio player manager][Default] delegate that registers
 * source managers on initialization using a [ServiceLoader].
 *
 * @author Kaidan Gustave
 */
open class ServiceAudioSourceManager : AudioPlayerManager by Default() {
    init {
        ServiceLoader.load(AudioSourceManager::class.java).forEach {
            registerSourceManager(it)
        }
    }
}