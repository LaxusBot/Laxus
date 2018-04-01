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

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import xyz.laxus.jda.util.embed
import xyz.laxus.util.progression
import kotlin.math.min
import kotlin.math.roundToInt

inline fun <reified R> AudioTrack.userData(): R? = userData as? R

val AudioTrack.member: Member get() = checkNotNull(userData()) {
    "User-data of type Member was null!"
}

fun Guild.trackEmbed(track: AudioTrack) = embed {
    color { selfMember.color }
    title { "$name `[${track.progression}]`" }
    val pos = track.position
    val dur = track.duration

    val per = (((pos.toDouble() * 100) / dur.toDouble()) / 10).roundToInt()
    repeat(per) {
        append('\u2588')
    }
    append("\uD83D\uDD18")
    repeat(10 - (min(per + 1, 10))) {
        append("\\_")
    }
}
