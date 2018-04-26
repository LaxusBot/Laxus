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

import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import xyz.laxus.db.DBStarEntries
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.embed
import xyz.laxus.jda.util.message
import xyz.laxus.util.Emojis
import xyz.laxus.util.formattedName
import java.awt.Color
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * @author Kaidan Gustave
 */
data class StarMessage(val starboard: Starboard, val starred: Message) {
    private companion object {
        private val msgBuilder = MessageBuilder()
    }

    lateinit var entry: Message
        private set

    val guild: Guild get() = starboard.guild

    val entryIsCreated get() = ::entry.isInitialized
    val starReactions get() = starred.stars
    val count get() = starred.starCount
    val starType get() = Emojis.Star.forCount(count)

    // Takes a user and adds them as a star to the message
    // If there is no star, we add it, otherwise we do nothing.
    suspend fun addStar(user: User) {
        starReactions.firstOrNull { it.userId == user.idLong } ?: DBStarEntries.addStar(
            starredId = starred.idLong,
            guildId = starred.guild.idLong,
            userId = user.idLong
        )

        if(entryIsCreated) {
            updateEntry()
        } else if(starboard.settings.threshold <= count) {
            createEntry()
        }
    }

    suspend fun createEntry() {
        val board = starboard.channel ?: return
        if(board.canTalk() && guild.selfMember.hasPermission(board, MESSAGE_EMBED_LINKS)) {
            msgBuilder.clear()
            val msg = message(msgBuilder) {
                append { this@StarMessage.toString() }
                setEmbed(createEmbed())
            }
            entry = board.sendMessage(msg).await()
            DBStarEntries.setEntry(entry.idLong, starred.idLong, guild.idLong)
        }
    }

    // Updates the starboard entry.
    // This should be called when a new star is added or removed.
    fun updateEntry() {
        when {
            !entryIsCreated -> return // entry isn't created yet
            count == 0 -> return starboard.deletedMessage(starred.idLong) // delete if no reactions left
            !entry.textChannel.canTalk() -> return
            !entry.guild.selfMember.hasPermission(entry.textChannel, MESSAGE_EMBED_LINKS) -> return
            else -> {
                msgBuilder.clear()
                val msg = message(msgBuilder) {
                    append { this@StarMessage.toString() }
                    setEmbed(createEmbed())
                }
                entry.editMessage(msg).queue()
            }
        }
    }

    fun isStarring(user: User): Boolean {
        return DBStarEntries.isStarring(starred.idLong, user.idLong, guild.idLong)
    }

    fun removeStar(user: User) {
        DBStarEntries.removeEntry(user.idLong, starred.idLong, guild.idLong)
        // Cleanup if it hits zero
        if(count <= 0) {
            println("test")
            // We go back up to the starboard first to remove it from the map.
            starboard.deletedMessage(starred.idLong)
        } else {
            updateEntry()
        }
    }

    fun delete() {
        if(entryIsCreated) {
            entry.delete().queue({}, {})
        }
        DBStarEntries.removeAllEntries(starred.idLong, guild.idLong)
    }

    override fun toString(): String {
        return "${starType.emoji} **$count** <#${starred.channel.idLong}> (ID: ${starred.idLong})"
    }

    private fun createEmbed() = embed {
        author {
            icon = starred.author.effectiveAvatarUrl
            value = starred.author.formattedName(false)
        }
        if(starred.contentRaw.isNotEmpty()) {
            + starred.contentRaw
        }
        if(starred.attachments.isNotEmpty()) {
            starred.attachments[0].takeIf { it.isImage }?.let { image { it.url } }
        }

        // Image embeds take precedence over attachments
        if(starred.embeds.isNotEmpty()) {
            image { starred.embeds[0].url }
        }
        color { Color(255, 255, (25.44 * min(count, 10)).roundToInt()) }
        time { starred.creationTime }
    }
}
