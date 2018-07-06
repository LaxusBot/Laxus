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
package xyz.laxus.command.standard

import com.jagrosh.jdautilities.commons.utils.FinderUtil
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.EmptyCommand
import xyz.laxus.command.Experiment
import xyz.laxus.jda.util.connectedChannel
import xyz.laxus.jda.util.findVoiceChannels
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.linkTo
import xyz.laxus.util.db.linkedChannel
import xyz.laxus.util.multipleVoiceChannels
import xyz.laxus.util.noMatch

class ChannelCommand: EmptyCommand(StandardGroup) {
    override val name = "Channel"
    override val help = "Manages various channel configurations."
    override val guildOnly = true

    private inner class ChannelInfoCommand: Command(this@ChannelCommand) {
        override val name = "Info"

        override suspend fun execute(ctx: CommandContext) {

        }
    }

    @Experiment("Channel linking is an experimental feature")
    private inner class ChannelLinkCommand: Command(this@ChannelCommand) {
        override val name = "Link"
        override val arguments = "<#TextChannel> [VoiceChannel]"
        override val help = "Creates a channel link between the voice channel and the text channel."
        override val defaultLevel = Level.ADMINISTRATOR

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args

            val textChannel: TextChannel
            val voiceChannel: VoiceChannel

            // If no args we will check to see if they're in a voice channel,
            //and link that in place of the argument.
            if(args.isEmpty()) {
                textChannel = ctx.textChannel
                voiceChannel = ctx.member.connectedChannel ?: return ctx.replyError {
                    "Must specify a VoiceChannel to be connected!"
                }
            } else {
                val split = args.split(commandArgs, 2)
                if(split.size > 1) {
                    val maybeTextChannelArg = split[0]
                    val match = ChannelRegex.matchEntire(maybeTextChannelArg)
                    textChannel = if(match === null) ctx.textChannel else {
                        val id = match.groupValues[1].toLong()
                        ctx.guild.getTextChannelById(id) ?: return ctx.replyError {
                            "Could not find text channel with ID: $id"
                        }
                    }
                    val voiceChannelArg = if(match === null) args else split[1]
                    val channels = ctx.guild.findVoiceChannels(voiceChannelArg)
                    voiceChannel = when {
                        channels.isEmpty() -> return ctx.replyError(noMatch("voice channels", voiceChannelArg))
                        channels.size > 1 -> return ctx.replyError(channels.multipleVoiceChannels(voiceChannelArg))
                        else -> channels[0]
                    }
                } else {
                    val channels = ctx.guild.findVoiceChannels(args)
                    textChannel = ctx.textChannel
                    voiceChannel = when {
                        channels.isEmpty() -> return ctx.replyError(noMatch("voice channels", args))
                        channels.size > 1 -> return ctx.replyError(channels.multipleVoiceChannels(args))
                        else -> channels[0]
                    }
                }
            }

            voiceChannel.linkedChannel?.let { previousLink ->
                return ctx.replyError(buildString {
                    append("${voiceChannel.name} is already linked to ${textChannel.asMention}!")
                    if(previousLink != textChannel) {
                        appendln()
                        append("Try to unlink the channel first using the ")
                        append("`${ctx.bot.prefix}${this@ChannelCommand.name} Unlink` command!")
                    }
                })
            }

            // link
            voiceChannel.linkTo(textChannel)

            // respond successfully!
            ctx.replySuccess("Successfully linked **${voiceChannel.name}** to ${textChannel.asMention}!")
        }
    }

    @Experiment("Channel linking is an experimental feature")
    private inner class ChannelUnlinkCommand: Command(this@ChannelCommand) {
        override val name = "Unlink"
        override val defaultLevel = Level.ADMINISTRATOR

        override suspend fun execute(ctx: CommandContext) {

        }
    }

    private companion object {
        private val ChannelRegex = FinderUtil.CHANNEL_MENTION.toRegex()
    }
}