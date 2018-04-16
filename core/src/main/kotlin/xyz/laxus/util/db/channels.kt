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

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import xyz.laxus.db.DBChannels
import xyz.laxus.db.DBChannels.Type.*
import xyz.laxus.db.DBWelcomes

var TextChannel.isIgnored: Boolean
    get() = DBChannels.isChannel(guild.idLong, idLong, IGNORED)
    set(value) {
        if(value) {
            DBChannels.addChannel(guild.idLong, idLong, IGNORED)
        } else {
            DBChannels.removeChannel(guild.idLong, idLong, IGNORED)
        }
    }

val Guild.ignoredChannels: List<TextChannel> get() {
    return DBChannels.getChannels(idLong, IGNORED).mapNotNull { getTextChannelById(it) }
}

var Guild.modLog: TextChannel?
    get() = getChannelTypeOf(MOD_LOG)
    set(value) = setChannelTypeOf(value, MOD_LOG)

val Guild.hasModLog: Boolean get() {
    return DBChannels.hasChannel(idLong, MOD_LOG)
}

var Guild.announcementChannel: TextChannel?
    get() = getChannelTypeOf(ANNOUNCEMENT)
    set(value) = setChannelTypeOf(value, ANNOUNCEMENT)

val Guild.hasAnnouncementChannel: Boolean get() {
    return DBChannels.hasChannel(idLong, ANNOUNCEMENT)
}

private fun Guild.getChannelTypeOf(type: DBChannels.Type): TextChannel? {
    return DBChannels.getChannel(idLong, type)?.let { getTextChannelById(it) }
}

private fun Guild.setChannelTypeOf(value: TextChannel?, type: DBChannels.Type) {
    if(value !== null) DBChannels.setChannel(idLong, value.idLong, type) else DBChannels.removeChannel(idLong, type)
}

val Guild.hasWelcome get() = DBWelcomes.hasWelcome(idLong)

var Guild.welcome: Pair<TextChannel, String>?
    get() {
        val welcome = DBWelcomes.getWelcome(idLong) ?: return null
        val channel = getTextChannelById(welcome.first) ?: return null
        return channel to welcome.second
    }
    set(value) {
        if(value === null) {
            DBWelcomes.removeWelcome(idLong)
        } else {
            DBWelcomes.setWelcome(idLong, value.first.idLong, value.second)
        }
    }