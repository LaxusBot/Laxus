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
package xyz.laxus.command.music

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.COMMON
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import kotlinx.coroutines.experimental.async
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Message
import xyz.laxus.Laxus
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.menus.orderedMenu
import xyz.laxus.jda.menus.orderedMenuBuilder
import xyz.laxus.music.MusicManager
import xyz.laxus.util.formattedInfo
import xyz.laxus.util.noMatch
import kotlin.coroutines.experimental.coroutineContext

/**
 * @author Kaidan Gustave
 */
class SearchCommand(manager: MusicManager): MusicCommand(manager) {
    override val name = "Search"
    override val arguments = "[Query]"
    override val help = "Searches for songs matching a query and plays in a voice channel."
    override val botPermissions = arrayOf(
        VOICE_CONNECT,
        VOICE_SPEAK
    )

    private val builder = orderedMenuBuilder {
        waiter { Laxus.Waiter }
        timeout { delay { 20 } }
        useNumbers { true }
        allowTextInput { true }
        useCancelButton { true }
    }

    override suspend fun execute(ctx: CommandContext) {
        val guild = ctx.guild
        val member = ctx.member
        val query = ctx.args
        val voiceChannel = ctx.voiceChannel

        if(!member.inPlayingChannel || voiceChannel === null) {
            if(guild.isPlaying) {
                return ctx.notInPlayingChannel()
            }
            if(voiceChannel === null) {
                return ctx.notInVoiceChannel()
            }
        }

        val loading = async(coroutineContext) {
            ctx.send("Loading...")
        }

        val item = try {
            loadTrack(member, query, isSearchList = true)
        } catch(e: FriendlyException) {
            val message = loading.await()
            return when(e.severity) {
                COMMON -> message.editMessage("An error occurred${e.message?.let { ": $it" } ?: ""}.").queue()
                else -> message.editMessage("An error occurred.").queue()
            }
        }

        when(item) {
            null -> ctx.replyWarning(noMatch("results", query))
            is AudioTrack -> ctx.singleTrackLoaded(loading.await(), item)
            is AudioPlaylist -> {
                val tracks = item.tracks.onEach { it.userData = member }
                if(!item.isSearchResult) {
                    ctx.playlistLoaded(loading.await(), item)
                } else {
                    builder.clearChoices()
                    val menu = orderedMenu(builder) {
                        user { ctx.author }
                        color { ctx.member.color }
                        text { "Results for \"$query\":" }
                        for(track in tracks.subList(0, 5)) {
                            val info = track.info.formattedInfo
                            this[info] = { message ->
                                clearReactionsCorrectly(message)
                                ctx.singleTrackLoaded(message, track)
                            }
                        }
                        finalAction { message -> clearReactionsCorrectly(message) }
                    }

                    menu.displayAs(loading.await())
                }
            }

            // This shouldn't happen, but...
            else -> unsupportedItemType(loading.await())
        }
    }

    private fun clearReactionsCorrectly(message: Message) {
        if(message.guild.selfMember.hasPermission(message.textChannel, MESSAGE_MANAGE)) {
            message.clearReactions().queue()
        } else {
            // Clear our reactions
            message.reactions.forEach { it.removeReaction().queue() }
        }
    }
}