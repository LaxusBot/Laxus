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
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceUpdateEvent
import xyz.laxus.auto.listener.AutoListener
import xyz.laxus.util.db.linkedChannel
import xyz.laxus.util.db.links
import xyz.laxus.util.db.unlinkTo

@AutoListener
class ChannelLinksListener {
    protected fun onReady(event: ReadyEvent) {
        event.jda.guildCache.forEach { guild ->
            for(voice in guild.voiceChannelCache) {
                val text = voice.linkedChannel?.takeIf { it.canManagePermissions() } ?: continue

                val override = checkNotNull(text.getPermissionOverride(guild.publicRole)) {
                    "Could not get permission override for public role???\n" +
                    "Guild: ${guild.name} (ID: ${guild.idLong})\n" +
                    "TextChannel: #${text.name} (ID: ${text.idLong})\n" +
                    "VoiceChannel: ${voice.name} (ID: ${voice.idLong})"
                }

                val denied = override.denied
                if(!denied.containsAll(perms)) {
                    override.manager.deny(perms).queue()
                }

                voice.members.forEach { enableLink(it, text) }
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
        enableLink(event.member, event.channelJoined.linkedChannel ?: return)
    }

    protected fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val voice = event.channelLeft
        val text = voice.linkedChannel?.takeIf { it.canManagePermissions() } ?: return

        text.getPermissionOverride(event.member)?.let {
            if((it.allowed - perms).isEmpty() && it.denied.isEmpty()) {
                it.delete().queue()
            } else {
                it.manager.clear(perms).queue()
            }
        }

        if(event is GuildVoiceMoveEvent) {
            enableLink(event.member, event.channelJoined.linkedChannel ?: return)
        }
    }

    private fun enableLink(member: Member, text: TextChannel) {
        if(!text.canManagePermissions()) return

        text.getPermissionOverride(member)?.let {
            return it.manager.grant(perms).queue()
        }

        text.createPermissionOverride(member).setAllow(perms).queue()
    }

    private fun TextChannel.canManagePermissions(): Boolean {
        return guild.selfMember.hasPermission(this, MANAGE_PERMISSIONS)
    }

    private companion object {
        private val perms = listOf(MESSAGE_READ, VIEW_CHANNEL)
    }
}
