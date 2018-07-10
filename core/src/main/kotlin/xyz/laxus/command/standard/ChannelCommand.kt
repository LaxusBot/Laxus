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
import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.entities.ChannelType.*
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.EmptyCommand
import xyz.laxus.jda.menus.paginator
import xyz.laxus.jda.menus.paginatorBuilder
import xyz.laxus.jda.util.*
import xyz.laxus.util.*
import xyz.laxus.util.db.linkTo
import xyz.laxus.util.db.linkedChannel
import xyz.laxus.util.db.unlinkTo

class ChannelCommand: EmptyCommand(StandardGroup) {
    override val name = "Channel"
    override val help = "Manages various channel configurations."
    override val guildOnly = true
    override val children = arrayOf(
        ChannelLinkCommand(),
        ChannelUnlinkCommand()
    )

    @Suppress("unused") // TODO
    private inner class ChannelInfoCommand: Command(this@ChannelCommand) {
        override val name = "Info"
        override val aliases = arrayOf("I", "Information")
        override val arguments = "<ChannelType> <Channel>"
        override val help = "Gets information on a specified channel."
        override val guildOnly = true

        override suspend fun execute(ctx: CommandContext) {
            @Suppress("ConstantConditionIf") if(true) return ctx.replyError {
                "This command is under construction!"
            }

            val args = ctx.args
            val channel: Channel = when {
                args.isEmpty() -> ctx.textChannel

                else -> {
                    val split = args.split(commandArgs, 2)
                    if(split.size > 1) {
                        val (first, second) = split
                        val channelType = ignored(null) {
                            ChannelType.valueOf(first.toUpperCase()).takeIf { it.isGuild }
                        } ?: return ctx.replyError {
                            "**$first** is not a valid channel type.\n" +
                            "Valid types are ${ChannelType.values().joinToString(",", transform = ChannelType::titleName)}."
                        }

                        val channels: List<Channel> = when(channelType) {
                            TEXT -> ctx.guild.findTextChannels(second)
                            VOICE -> ctx.guild.findVoiceChannels(second)
                            CATEGORY -> ctx.guild.findCategories(second)
                            else -> throw IllegalStateException("No handle for channel type: $channelType")
                        }

                        when {
                            channels.isEmpty() -> return ctx.replyError(noMatch("${channelType.name.toLowerCase()} channels", second))
                            channels.size > 1 -> @Suppress("UNCHECKED_CAST") (return when(channelType) {
                                TEXT -> ctx.replyError((channels as List<TextChannel>).multipleTextChannels(second))
                                VOICE -> ctx.replyError((channels as List<VoiceChannel>).multipleVoiceChannels(second))
                                CATEGORY -> ctx.replyError((channels as List<Category>).multipleCategories(second))
                            // what?
                                else -> throw IllegalStateException("What?")
                            })
                            else -> channels[0]
                        }
                    } else {
                        val channels = ctx.guild.findTextChannels(args)
                        when {
                            channels.isEmpty() -> return ctx.replyError(noMatch("text channels", args))
                            channels.size > 1 -> return ctx.replyError(channels.multipleTextChannels(args))
                            else -> channels[0]
                        }
                    }
                }
            }

            val embed = embed {
                when(channel) {
                    is TextChannel -> {
                        title { "Info on #${channel.name}" }
                        channel.topic?.let { append(it) }
                    }

                    // Nani. The. Fuck.
                    else -> throw IllegalStateException("WHAT?")
                }

                footer { value { "Created on" } }
                time { channel.creationTime }
            }

            ctx.reply(embed)
        }
    }

    private inner class ChannelLinkCommand: Command(this@ChannelCommand) {
        override val name = "Link"
        override val arguments = "<#TextChannel> [VoiceChannel]"
        override val help = "Creates a channel link between the voice channel and the text channel."
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(MANAGE_PERMISSIONS, MANAGE_CHANNEL)
        override val children = arrayOf(ChannelLinkListCommand())

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
                    val match = channelRegex.matchEntire(maybeTextChannelArg)
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

            // Create a permission override
            val override = checkNotNull(voiceChannel.getPermissionOverride(ctx.guild.publicRole))

            // Set the perms to be denied to normal members
            override.manager.deny(linkedPermissions).queue()

            // update members in the voice channel already
            for(member in voiceChannel.members) {
                // Get the override for the member in the linked text channel
                val memberOverride = textChannel.getPermissionOverride(member)
                if(memberOverride !== null) {
                    // Grant the perms necessary to read the linked text channel
                    memberOverride.manager.grant(linkedPermissions).queue()
                    continue
                }
                textChannel.createPermissionOverride(member).setAllow(linkedPermissions).queue()
            }

            // respond successfully
            ctx.replySuccess("Successfully linked **${voiceChannel.name}** to ${textChannel.asMention}!")
        }

        private inner class ChannelLinkListCommand: Command(this@ChannelLinkCommand) {
            override val name = "List"
            override val help = "Lists the server's linked channels."
            override val guildOnly = true
            override val defaultLevel = Level.STANDARD
            override val botPermissions = arrayOf(
                MESSAGE_EMBED_LINKS,
                MESSAGE_MANAGE,
                MESSAGE_ADD_REACTION
            )

            private val builder = paginatorBuilder {
                waiter { Laxus.Waiter }
                itemsPerPage { 6 }
                waitOnSinglePage { false }
                numberItems { true }
                showPageNumbers { true }
            }

            override suspend fun execute(ctx: CommandContext) {
                val links = ctx.guild.voiceChannelCache.asSequence()
                    .mapNotNull { voice -> voice.linkedChannel?.let { text -> voice to text } }
                    .toMap()

                builder.clearItems()
                val paginator = paginator(builder) {
                    text { p, t -> "Channel Links for **${ctx.guild.name}** (Page $p/$t)" }
                    color { _, _ -> ctx.selfMember.color }
                    links.forEach { voice, text ->
                        add { "**${voice.name}** -> ${text.asMention}" }
                    }
                    finalAction {
                        ctx.linkMessage(it)
                        if(ctx.selfMember.hasPermission(ctx.textChannel, MESSAGE_MANAGE)) {
                            it.clearReactions().queue()
                        }
                    }
                }

                paginator.displayIn(ctx.channel)
            }
        }
    }

    private inner class ChannelUnlinkCommand: Command(this@ChannelCommand) {
        override val name = "Unlink"
        override val arguments = "[VoiceChannel]"
        override val help = "Removes a channel link between the voice channel and it's linked text channel."
        override val defaultLevel = Level.ADMINISTRATOR
        override val botPermissions = arrayOf(MANAGE_PERMISSIONS, MANAGE_CHANNEL)

        override suspend fun execute(ctx: CommandContext) {
            val args = ctx.args

            val voiceChannel = when {
                args.isEmpty() -> ctx.member.connectedChannel ?: return ctx.replyError {
                    "Must specify a VoiceChannel to be connected!"
                }

                else -> {
                    val channels = ctx.guild.findVoiceChannels(args)
                    when {
                        channels.isEmpty() -> return ctx.replyError(noMatch("voice channels", args))
                        channels.size > 1 -> return ctx.replyError(channels.multipleVoiceChannels(args))
                        else -> channels[0]
                    }
                }
            }

            val link = voiceChannel.linkedChannel ?: return ctx.replyError {
                "**${voiceChannel.name}** does not have a linked text channel!"
            }

            // Unlink
            voiceChannel.unlinkTo(link)

            // Handle existing permission override

            // Get permission override for link
            link.getPermissionOverride(ctx.guild.publicRole)?.manager?.clear(linkedPermissions)?.queue()

            // Handle any members
            voiceChannel.members.forEach {
                // Get the member's override for the linked text channel
                link.getPermissionOverride(it)?.let {
                    // If the allowed perms minus the granted perms is empty
                    //and the denied perms are empty, we will just delete the
                    //entire override so we don't leave behind unused overrides.
                    if((it.allowed - linkedPermissions).isEmpty() && it.denied.isEmpty()) {
                        it.delete().queue()
                    } else {
                        // Otherwise we just set the perms we granted back to inherited
                        it.manager.clear(linkedPermissions).queue()
                    }
                }
            }

            // Respond
            ctx.replySuccess("Successfully unlinked **${voiceChannel.name}** to ${link.asMention}!")
        }
    }

    private companion object {
        private val channelRegex = FinderUtil.CHANNEL_MENTION.toRegex()
        private val linkedPermissions = listOf(MESSAGE_READ, VIEW_CHANNEL)
    }
}