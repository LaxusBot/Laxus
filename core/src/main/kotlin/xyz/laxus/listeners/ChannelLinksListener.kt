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
package xyz.laxus.listeners

import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.PermissionOverride
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceUpdateEvent
import xyz.laxus.auto.listener.AutoListener
import xyz.laxus.util.createLogger
import xyz.laxus.util.db.linkedChannel
import xyz.laxus.util.db.links
import xyz.laxus.util.db.unlinkTo
import xyz.laxus.util.formattedName

@AutoListener
class ChannelLinksListener {
    protected fun onReady(event: ReadyEvent) {
        // for each guild
        event.jda.guildCache.forEach { guild ->
            // for every voice channel
            for(voice in guild.voiceChannelCache) {
                // Get the linked text channel.
                // If it cannot be managed, skip it.
                val text = voice.linkedChannel?.takeIf { it.canManagePermissions() } ?: continue

                // Get the permission override for the public role if one already exists
                val override = text.getPermissionOverride(guild.publicRole)

                // if the override exists
                if(override !== null) {
                    // If the override does not have all the denied permissions
                    //we should go ahead and deny those
                    if(!override.denied.containsAll(perms)) {
                        override.manager.deny(perms).queue()
                    }
                } else {
                    // if the override doesn't exist in the first place
                    //create one that denies the permissions
                    text.createPermissionOverride(guild.publicRole).setDeny(perms).queue()
                }

                // linked members
                val members = voice.members

                // For every member in the voice channel, enable
                //the link to the linked text channel
                members.forEach { enableLink(it, text) }

                // Cleanup any preexisting overrides, possibly left
                //unhandled during a restart of the bot?
                text.memberPermissionOverrides.asSequence()
                    .filter { it.member !in members }
                    .forEach { handleUnlink(it, true) }
            }
        }
    }

    protected fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        val channel = event.channel
        channel.links.forEach { it.unlinkTo(channel) }
    }

    protected fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        val channel = event.channel
        channel.linkedChannel?.let { channel.unlinkTo(it) }
    }

    protected fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        // on join, enable link
        enableLink(event.member, event.channelJoined.linkedChannel ?: return)
    }

    protected fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        // get the channel that was left
        val voice = event.channelLeft

        // get the linked text channel that should be unlinked to the member
        // if there is none, we will return here
        val text = voice.linkedChannel?.takeIf { it.canManagePermissions() } ?: return

        // get the permission override for the member if one exists
        text.getPermissionOverride(event.member)?.let { handleUnlink(it) }

        // If this event is a move event, we need to enable the
        //link to the joined channel afterwards.
        if(event is GuildVoiceMoveEvent) {
            enableLink(event.member, event.channelJoined.linkedChannel ?: return)
        }
    }

    private fun handleUnlink(override: PermissionOverride, isLate: Boolean = false) {
        // To simplify bot usage for things like music
        //bots, we skip the override if it's for a bot.
        if(override.member.user.isBot) return

        // The override should be deleted if the currently allowed permissions
        //are only those that are granted by the link. It should also have no
        //denied permissions.
        val shouldBeDeleted = (override.allowed - perms).isEmpty() &&
                              override.denied.isEmpty()

        // if it should be deleted, delete the permission
        if(shouldBeDeleted) override.delete().queue() else if(!isLate) {
            // Otherwise clear the linked permissions
            //and maintain the override afterwards.
            override.manager.clear(perms).queue()
        }
    }

    private fun enableLink(member: Member, text: TextChannel) {
        // no bots
        if(member.user.isBot) return
        if(!text.canManagePermissions()) return

        text.getPermissionOverride(member)?.let { override ->
            return override.manager.grant(perms).queue({}) {
                log.warn(
                    "Encountered an issue while enabling link " +
                    "for ${member.user.formattedName()} in " +
                    "#${text.name} (ID: ${text.idLong}) on guild " +
                    "with ID ${text.guild.idLong}"
                )
            }
        }

        text.createPermissionOverride(member).setAllow(perms).queue({}) {
            log.warn(
                "Encountered an issue while enabling link " +
                "for ${member.user.formattedName()} in " +
                "#${text.name} (ID: ${text.idLong}) on guild " +
                "with ID ${text.guild.idLong}"
            )
        }
    }

    private fun TextChannel.canManagePermissions(): Boolean {
        return guild.selfMember.hasPermission(this, MANAGE_PERMISSIONS)
    }

    private companion object {
        private val log = createLogger("ChannelLinks")

        private val perms = listOf(MESSAGE_READ, VIEW_CHANNEL)
    }
}
