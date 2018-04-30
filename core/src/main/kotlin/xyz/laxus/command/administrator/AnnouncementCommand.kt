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
@file:Suppress("unused") // TODO

package xyz.laxus.command.administrator

import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.*
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.announcementRoles
import xyz.laxus.util.db.announcementsChannel
import xyz.laxus.util.multipleRoles
import xyz.laxus.util.multipleTextChannels
import xyz.laxus.util.noMatch

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Specify a role to announce to, and a message.")
class AnnouncementCommand: Command(AdministratorGroup) {
    override val name = "Announcement"
    override val aliases = arrayOf("Announce")
    override val arguments = "[Role] [Message]"
    override val help = "Mentions a role with a message in the server's announcements channel."
    override val children = arrayOf(
        AnnouncementAddRoleCommand(),
        AnnouncementChannelCommand()
    )

    override suspend fun execute(ctx: CommandContext) {
        val channel = ctx.guild.announcementsChannel ?: return ctx.replyError {
            "No announcement channel has been set for this server!"
        }

        if(!channel.canTalk()) return ctx.replyError {
            "I cannot announce to ${channel.asMention} because I do not have permission to send messages there!"
        }

        val split = ctx.args.split(commandArgs, 2)

        if(split.size < 2) {
            return ctx.error("Too Few Arguments") {
                "Specify a role followed by a message to announce!"
            }
        }

        val roleQuery = split[0]
        val message = split[1]
        if(message.isEmpty()) return ctx.replyError {
            "Message was blank, you must specify a message after the role to announce to!"
        }

        val roles = ctx.guild.announcementRoles
        val found = ctx.guild.findRoles(roleQuery).filter { it in roles }

        val target = when {
            found.isEmpty() -> return ctx.replyError(noMatch("announcement roles", ctx.args))
            found.size > 1 -> return ctx.replyError(found.multipleRoles(ctx.args))
            else -> found[0]
        }
        if(target.isMentionable) {
            announce(ctx, target, channel, message)
        } else {
            try {
                target.edit {
                    mentionable { true }
                }.await()
            } catch(t: Throwable) {
                return ctx.replyError {
                    "An error occurred when sending the announcement to ${channel.asMention}!"
                }
            }
            announce(ctx, target, channel, message)
            target.edit { mentionable { false } }.await()
        }
    }

    private suspend fun announce(ctx: CommandContext, role: Role, channel: TextChannel, message: String) {
        val msgs = CommandContext.processMessage(message)
        if(msgs.size > 2) return ctx.replyError {
            "Announcement is too long! Announcements can only be a maximum of 4000 characters!"
        }

        try {
            for((index, msg) in msgs.withIndex()) {
                channel.sendMessage(message {
                    if(index == 0) append(role).appendln()
                    append(msg)
                }).await()
                if(index == msgs.size - 1) {
                    ctx.replySuccess("Successfully sent announcement to ${channel.asMention}!")
                }
            }
        } catch(t: Throwable) {
            ctx.replyError("An error occurred while sending the announcement to ${channel.asMention}!")
        }
    }

    @MustHaveArguments("Specify a channel to use as this server's announcements channel!")
    private inner class AnnouncementChannelCommand: Command(this@AnnouncementCommand) {
        override val name = "Channel"
        override val arguments = "[Channel]"
        override val help = "Sets the channel to send announcements to."

        override suspend fun execute(ctx: CommandContext) {

            val args = ctx.args

            val channels = ctx.guild.findTextChannels(args)

            val channel = when {
                channels.isEmpty() -> return ctx.replyError(noMatch("channels", args))
                channels.size > 1 -> return ctx.replyError(channels.multipleTextChannels(args))
                else -> channels[0]
            }

            val current = ctx.guild.announcementsChannel

            if(channel == current) return ctx.replyError {
                "${channel.asMention} is already the announcement channel for this server!"
            }

            if(!channel.canTalk()) return ctx.replyError {
                "${channel.asMention} cannot be set as the announcement channel because " +
                "I do not have permission to send messages there!"
            }

            ctx.guild.announcementsChannel = channel
            ctx.replySuccess("Successfully set ${channel.asMention} as the server's announcement channel!")
        }
    }

    private inner class AnnouncementAddRoleCommand: Command(this@AnnouncementCommand) {
        override val name = "AddRole"

        override suspend fun execute(ctx: CommandContext) {

        }
    }
}