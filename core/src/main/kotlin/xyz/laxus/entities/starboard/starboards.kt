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
package xyz.laxus.entities.starboard

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import xyz.laxus.db.entities.StarSettings
import xyz.laxus.db.DBStarEntries
import xyz.laxus.db.DBStarSettings

val Guild.starboard: Starboard? get() {
    return StarboardManager.getStarboard(this)
}

val Guild.hasStarboard: Boolean get() {
    return DBStarSettings.hasSettings(idLong)
}

var Guild.starboardSettings: StarSettings?
    get() = DBStarSettings.getSettings(idLong)
    set(value) {
        if(value !== null) {
            require(value.guildId == idLong) { "Expected value to have ID: $idLong but found ${value.guildId}" }
            value.update()
        } else DBStarSettings.removeSettings(idLong)
    }

var Guild.starboardChannel: TextChannel?
    get() = DBStarSettings.getSettings(idLong)?.channelId?.let { getTextChannelById(it) }
    set(value) {
        val channel = requireNotNull(value) { "Cannot set starboardChannel to null, use starboardSettings instead!" }
        val settings = starboardSettings?.also {
            it.channelId = channel.idLong
        } ?: StarSettings(idLong, channel.idLong)
        settings.update()
    }

val Message.stars: List<Star> get() {
    requireNotNull(guild) { "Cannot get stars for a Message with null guild!" }
    return DBStarEntries.getStars(idLong, guild.idLong).map { Star(this, it) }
}

val Message.starCount: Int get() {
    requireNotNull(guild) { "Cannot get stars for a Message with null guild!" }
    return DBStarEntries.getStarCount(idLong, guild.idLong)
}

fun Guild.removeStarboard() {
    StarboardManager.removeStarboard(this)
}
