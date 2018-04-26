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
import xyz.laxus.Laxus
import xyz.laxus.jda.KEmbedBuilder
import xyz.laxus.jda.util.embed
import xyz.laxus.util.progression
import kotlin.math.min
import kotlin.math.roundToInt

inline fun <reified R> AudioTrack.userData(): R? = userData as? R

val AudioTrack.member: Member get() = checkNotNull(userData()) {
    "User-data of type Member was null!"
}

fun KEmbedBuilder.trackEmbed(guild: Guild, track: AudioTrack, next: AudioTrack?, paused: Boolean) {
    color { guild.selfMember.color }
    title { track.info.title }
    url { track.info.uri }

    if(paused) {
        append('\u23F8')
    } else {
        append('\u25B6')
    }
    append(" __[`")
    val pos = track.position
    val dur = track.duration
    val per = (((pos.toDouble() * 100) / dur.toDouble()) / 10).roundToInt()
    repeat(per) { append('\u2588') }
    append('\u2B1C')
    repeat(10 - min(per, 10)) { append('_') }
    append("`](${Laxus.ServerInvite})__ `[${track.progression}]`")
    if(next !== null && per >= 8 && !paused) {
        footer {
            value { "Next up: ${next.info.title}" }
            url { next.info.uri }
        }
    }
}