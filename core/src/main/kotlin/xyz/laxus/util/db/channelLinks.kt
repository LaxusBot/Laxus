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
package xyz.laxus.util.db

import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import xyz.laxus.db.DBChannelLinks
import xyz.laxus.db.entities.ChannelLink

val VoiceChannel.linkedChannel: TextChannel? get() {
    DBChannelLinks.getVoiceChannelLink(guild.idLong, idLong)?.let {
        val channel = guild.getTextChannelById(it.textChannelId)
        if(channel === null) {
            DBChannelLinks.removeChannelLink(it)
        }
        return channel
    }
    return null
}

val VoiceChannel.hasLink: Boolean get() {
    return DBChannelLinks.getVoiceChannelLink(guild.idLong, idLong) !== null // FIXME Maybe add DB method?
}

fun VoiceChannel.isLinkedTo(text: TextChannel): Boolean {
    return DBChannelLinks.isLinked(guild.idLong, idLong, text.idLong)
}

fun VoiceChannel.linkTo(text: TextChannel) {
    val link = ChannelLink(guild.idLong, text.idLong, idLong)
    DBChannelLinks.setChannelLink(link)
}

fun VoiceChannel.unlinkTo(text: TextChannel) {
    val link = ChannelLink(guild.idLong, text.idLong, idLong)
    DBChannelLinks.removeChannelLink(link)
}

val TextChannel.links: List<VoiceChannel> get() {
    return DBChannelLinks.getTextChannelLinks(guild.idLong, idLong).mapNotNull {
        val channel = guild.getVoiceChannelById(it.voiceChannelId)
        if(channel === null) {
            DBChannelLinks.removeChannelLink(it)
        }
        return@mapNotNull channel
    }
}