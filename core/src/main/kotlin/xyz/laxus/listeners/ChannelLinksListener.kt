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
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.laxus.auto.listener.AutoListener
import xyz.laxus.util.db.linkedChannel

@AutoListener
class ChannelLinksListener {
    protected fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val voice = event.channelLeft
        val text = voice.linkedChannel ?: return

        if(!event.guild.selfMember.hasPermission(text, MANAGE_PERMISSIONS)) return

        val override = text.getPermissionOverride(event.member)
        if(override !== null) {
            val after = override.allowed - Perms
            if(after.isEmpty()) {
                override.delete().queue({}, {})
            } else {
                override.manager.clear(Perms).queue({}, {})
            }
        }

        if(event is GuildVoiceMoveEvent) {
            enableLink(event.member, event.channelJoined)
        }
    }

    private fun enableLink(member: Member, voice: VoiceChannel) {
        val text = voice.linkedChannel ?: return
        if(!member.guild.selfMember.hasPermission(text, MANAGE_PERMISSIONS)) return

        text.getPermissionOverride(member)?.let {
            return it.manager.grant(Perms).queue()
        }

        text.createPermissionOverride(member).setAllow(Perms).queue()
    }

    private companion object {
        private val Perms = listOf(MESSAGE_READ)
    }
}