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
@file:Suppress("MemberVisibilityCanBePrivate")
package xyz.laxus.entities.starboard

// We'll probably end up doing changing this one day, but for now some notes on
// why we handle starboard WAY differently then say, RoleMe roles, or ModLog channels
// is because internally it's pretty strict.
//
// Basic concept:
//
// If the guild has a starboard, we will generate a Starboard object and cache it.
// This is effective immediately upon the first "star reaction" being added to a
// message on a guild where we have database settings for the starboard.

import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveAllEvent
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent
import xyz.laxus.db.entities.StarSettings
import xyz.laxus.jda.listeners.SuspendedListener
import xyz.laxus.jda.util.await
import xyz.laxus.util.collections.concurrentHashMap
import xyz.laxus.util.ignored

/**
 * @author Kaidan Gustave
 */
object StarboardManager : SuspendedListener {
    private val starboards = concurrentHashMap<Long, Starboard>()

    fun createStarboard(starboard: TextChannel): Starboard {
        val guild = starboard.guild
        if(!guild.hasStarboard) {
            guild.starboardSettings = StarSettings(guild.idLong, starboard.idLong)
        }

        return requireNotNull(getStarboard(guild)) {
            "Created starboard settings for Guild (ID: ${guild.idLong}) but could not " +
            "immediately create Starboard instance!"
        }
    }

    fun removeStarboard(guild: Guild) {
        starboards.remove(guild.idLong)

        if(guild.hasStarboard) {
            guild.starboardSettings = null
        }
    }

    fun getStarboard(guild: Guild): Starboard? {
        val settings = guild.starboardSettings
        if(settings === null) {
            if(guild.hasStarboard) {
                guild.starboardSettings = null
            }
            return null
        }
        return starboards.computeIfAbsent(guild.idLong) { Starboard(guild, settings) }
    }

    override suspend fun onEvent(event: Event) {
        if(event !is GenericGuildMessageEvent)
            return

        val guild = event.guild
        val starboard = getStarboard(guild) ?: return

        if(!guild.selfMember.hasPermission(event.channel, MESSAGE_MANAGE))
            return

        if(event is GuildMessageDeleteEvent) {
            starboard[event.messageIdLong]?.delete()
        } else if(event is GenericGuildMessageReactionEvent) {

            val reactionName = event.reaction.reactionEmote.name

            val isValid = when(reactionName) {
                "\u2B50", "\uD83C\uDF1F", "\uD83D\uDCAB" -> true
                else -> reactionName.contains("star", ignoreCase = true)
            }

            if(!isValid) return

            when(event) {
                is GuildMessageReactionAddEvent -> {
                    val starMessage = starboard[event.messageIdLong]
                    if(starMessage === null) {
                        val message = event.channel.getMessageById(event.messageIdLong).await()
                        message?.let { starboard.addStar(event.user, message) }
                    } else {
                        if(starMessage.isStarring(event.user))
                            return
                        starMessage.addStar(event.user)
                    }
                }

                is GuildMessageReactionRemoveEvent -> {
                    // Shouldn't ever be null
                    val starMessage = starboard[event.messageIdLong] ?: return
                    val updated = ignored(null) { event.channel.getMessageById(event.messageIdLong).await() } ?: return

                    var noneRemain = true
                    for(reaction in updated.reactions) {
                        if(reaction.users.any(event.user::equals)) {
                            noneRemain = false
                            break
                        }
                    }

                    if(noneRemain) {
                        starMessage.removeStar(event.user)
                    }
                }
            }
        } else if(event is GuildMessageReactionRemoveAllEvent) {
            // If all the reactions are removed we just delete the message
            // because that means that all the reactions were removed.
            starboard.deletedMessage(event.messageIdLong)
        }
    }
}
