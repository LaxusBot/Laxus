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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.laxus.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.core.entities.Member
import xyz.laxus.music.lava.userData

/**
 * @author Kaidan Gustave
 */
@Deprecated(
    message = "Not a viable replacement for userData",
    replaceWith = ReplaceWith(
        expression = "AudioTrack.userData"
    ),
    level = DeprecationLevel.ERROR
)
class MemberTrack(member: Member, internal val originalTrack: AudioTrack): AudioTrack by originalTrack {
    init {
        userData = member
    }

    val member: Member get() = requireNotNull(userData()) {
        "User Data was not a Member instance, possibly overwritten?"
    }
}
