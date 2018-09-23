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
package xyz.laxus.bot.utils.db

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import xyz.laxus.db.DBChannels
import xyz.laxus.db.DBChannels.Type.*
import xyz.laxus.db.DBWelcomes

var TextChannel.isIgnored: Boolean
    get() = isChannelTypeOf(IGNORED)
    set(value) = addOrRemoveChannelTypeOf(value, IGNORED)
val Guild.ignoredChannels get() = getChannelsTypeOf(IGNORED)

var Guild.modLog: TextChannel?
    get() = getChannelTypeOf(MOD_LOG)
    set(value) = setChannelTypeOf(value, MOD_LOG)
val Guild.hasModLog get() = hasChannelTypeOf(MOD_LOG)
val TextChannel.isModLog get() = isChannelTypeOf(MOD_LOG)

var Guild.announcementsChannel: TextChannel?
    get() = getChannelTypeOf(ANNOUNCEMENTS)
    set(value) = setChannelTypeOf(value, ANNOUNCEMENTS)
val Guild.hasAnnouncementsChannel get() = hasChannelTypeOf(ANNOUNCEMENTS)
val TextChannel.isAnnouncementsChannel get() = isChannelTypeOf(MOD_LOG)

private fun TextChannel.isChannelTypeOf(type: DBChannels.Type): Boolean {
    return DBChannels.isChannel(guild.idLong, idLong, type)
}

private fun TextChannel.addOrRemoveChannelTypeOf(value: Boolean, type: DBChannels.Type) {
    if(value) {
        DBChannels.addChannel(guild.idLong, idLong, type)
    } else {
        DBChannels.removeChannel(guild.idLong, idLong, type)
    }
}

private fun Guild.hasChannelTypeOf(type: DBChannels.Type): Boolean {
    return DBChannels.hasChannel(idLong, type)
}

private fun Guild.getChannelTypeOf(type: DBChannels.Type): TextChannel? {
    return DBChannels.getChannel(idLong, type)?.let {
        val channel = getTextChannelById(it)
        if(channel === null) {
            DBChannels.removeChannel(idLong, type)
        }
        return@let channel
    }
}

private fun Guild.getChannelsTypeOf(type: DBChannels.Type): List<TextChannel> {
    return DBChannels.getChannels(idLong, IGNORED).mapNotNull {
        val channel = getTextChannelById(it)
        if(channel === null) {
            DBChannels.removeChannel(idLong, it, type)
        }
        return@mapNotNull channel
    }
}

private fun Guild.setChannelTypeOf(value: TextChannel?, type: DBChannels.Type) {
    if(value !== null) DBChannels.setChannel(idLong, value.idLong, type) else DBChannels.removeChannel(idLong, type)
}

// Welcomes

val Guild.hasWelcome get() = DBWelcomes.hasWelcome(idLong)

var Guild.welcome: Pair<TextChannel, String>?
    get() {
        val welcome = DBWelcomes.getWelcome(idLong)
        if(welcome === null) {
            if(hasWelcome) {
                DBWelcomes.removeWelcome(idLong)
            }
            return null
        }
        val channel = getTextChannelById(welcome.first)
        if(channel === null) {
            if(hasWelcome) {
                DBWelcomes.removeWelcome(idLong)
            }
            return null
        }
        return channel to welcome.second
    }
    set(value) {
        if(value === null) {
            DBWelcomes.removeWelcome(idLong)
        } else {
            DBWelcomes.setWelcome(idLong, value.first.idLong, value.second)
        }
    }
