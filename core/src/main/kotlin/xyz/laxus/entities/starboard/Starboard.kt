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
import net.dv8tion.jda.core.entities.User
import xyz.laxus.db.entities.StarSettings
import java.time.OffsetDateTime.*

/**
 * @author Kaidan Gustave
 */
class Starboard(val guild: Guild, settings: StarSettings): MutableMap<Long, StarMessage> by HashMap() {
    val settings = settings
        get() = guild.starboardSettings ?: field

    var channel: TextChannel?
        get() = guild.starboardChannel
        set(value) {
            if(value !== null) guild.starboardChannel = value
            else StarboardManager.removeStarboard(guild)
        }

    suspend fun addStar(user: User, starred: Message) {
        // Message is from starboard, so we don't do anything
        if(starred.channel == channel) {
            return
        }

        // Message is from NSFW channel
        if(starred.textChannel.isNSFW) {
            return
        }

        // Channel is ignored
        if(starred.channel.idLong in settings.ignored) {
            return
        }

        // Message is older than allowed
        if(starred.creationTime.plusHours(settings.maxAge.toLong()).isBefore(now())) {
            return
        }

        val starredMessage = computeIfAbsent(starred.idLong) { StarMessage(this, starred) }

        starredMessage.addStar(user)
    }

    fun deletedMessage(messageId: Long) {
        remove(messageId)?.delete()
    }
}
