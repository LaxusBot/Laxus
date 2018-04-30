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

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.channel.category.CategoryCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.laxus.db.DBGuildStore
import xyz.laxus.entities.starboard.hasStarboard
import xyz.laxus.entities.starboard.removeStarboard
import xyz.laxus.entities.starboard.starboardSettings
import xyz.laxus.jda.util.muteRole
import xyz.laxus.jda.util.refreshMutedRole
import xyz.laxus.util.db.*
import java.time.OffsetDateTime

/**
 * @author Kaidan Gustave
 */
object DatabaseListener: EventListener {
    override fun onEvent(event: Event) {
        when(event) {
            is ReadyEvent -> onReady(event)
            is RoleDeleteEvent -> onRoleDelete(event)
            is TextChannelCreateEvent -> onTextChannelCreate(event)
            is TextChannelDeleteEvent -> onTextChannelDelete(event)
            is VoiceChannelCreateEvent -> onVoiceChannelCreate(event)
            is CategoryCreateEvent -> onCategoryCreate(event)
            is GuildJoinEvent -> onGuildJoin(event)
            is GuildLeaveEvent -> onGuildLeave(event)
            is GuildMemberJoinEvent -> onGuildMemberJoin(event)
            is GuildMemberLeaveEvent -> onGuildMemberLeave(event)
        }
    }

    private fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach { guild ->
            guild.mutedRole?.let { guild.refreshMutedRole(it) }
        }
        cleanDatabase(event.jda)
    }

    private fun onRoleDelete(event: RoleDeleteEvent) {
        val role = event.role
        val guild = event.guild

        // RoleMe Deleted
        role.tryRemoveRoleMe()

        // ColorMe Deleted
        role.tryRemoveColorMe()

        // Announcement role
        role.tryRemoveAnnouncements()

        // Ignored role
        role.tryRemoveIgnored()

        // Mod Role Deleted
        guild.tryRemoveModRole(role)

        // Muted Role Deleted
        guild.tryRemoveMutedRole(role)
    }

    private fun onTextChannelCreate(event: TextChannelCreateEvent) {
        event.guild.mutedRole?.let { event.channel.muteRole(it) }
    }

    private fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        event.guild.mutedRole?.let { event.channel.muteRole(it) }
    }

    private fun onCategoryCreate(event: CategoryCreateEvent) {
        event.guild.mutedRole?.let { event.category.muteRole(it) }
    }

    // If the guild has a type of channel and it equals the deleted channel, then it's removed
    // if the type of channel is null, but the database contains info regarding that type, it
    // is also removed
    private fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        val channel = event.channel
        val guild = event.guild

        // Ignored Channel Deleted
        if(channel.isIgnored) {
            channel.isIgnored = false
        }

        // ModLog Deleted
        val modLog = guild.modLog
        if(modLog !== null) {
            if(modLog == channel) {
                guild.modLog = null
            }
        } else {
            if(guild.hasModLog) {
                guild.modLog = null
            }
        }

        // Welcome Channel Deleted
        val welcomeChan = guild.welcome?.first
        if(welcomeChan !== null) {
            if(welcomeChan == channel) {
                guild.welcome = null
            }
        } else {
            if(guild.hasWelcome) {
                guild.welcome = null
            }
        }

        // Announcements Channel Deleted
        val announceChan = guild.announcementsChannel
        if(announceChan !== null) {
            if(announceChan == channel) {
                guild.announcementsChannel = null
            }
        } else {
            if(guild.hasAnnouncementsChannel) {
                guild.announcementsChannel = null
            }
        }

        val starboard = guild.starboardSettings
        if(starboard !== null) {
            if(starboard.channelId == channel.idLong) {
                guild.removeStarboard()
            }
        } else {
            if(guild.hasStarboard) {
                guild.removeStarboard()
            }
        }
    }

    private fun onGuildJoin(event: GuildJoinEvent) {
        if(event.guild.selfMember.joinDate.plusMinutes(10).isAfter(OffsetDateTime.now())) {
            event.guild.memberCache.forEach {
                it.unregisterRolePersist()
            }
            DBGuildStore.addGuild(event.jda.shardInfo?.shardId?.toShort(), event.guild.idLong)
        }
    }

    private fun onGuildLeave(event: GuildLeaveEvent) {
        DBGuildStore.removeGuild(event.guild.idLong)
    }

    private fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        val member = event.member
        val isRolePersist = event.guild.isRolePersist
        if(isRolePersist) {
            member.registerRolePersist()
        }
    }

    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val member = event.member
        val rolePersist = if(!event.guild.isRolePersist) emptyList() else member.rolePersist
        if(rolePersist.isNotEmpty()) {
            val roles = member.rolePersist
            event.guild.controller.addRolesToMember(member, roles).queue()
            member.unregisterRolePersist()
        }
    }

    private fun cleanDatabase(jda: JDA) {
        val shardId = jda.shardInfo?.shardId?.toShort()
        val guildIds = if(shardId !== null) jda.asBot().shardManager.guildCache else jda.guildCache
        DBGuildStore.clearNonGuilds(guildIds.mapTo(HashSet()) { it.idLong })
    }

    private fun Role.tryRemoveRoleMe() {
        if(isRoleMe) {
            isRoleMe = false
        }
    }

    private fun Role.tryRemoveColorMe() {
        if(isColorMe) {
            isColorMe = false
        }
    }

    private fun Role.tryRemoveAnnouncements() {
        if(isAnnouncements) {
            isAnnouncements = false
        }
    }

    private fun Role.tryRemoveIgnored() {
        if(isIgnored) {
            isIgnored = false
        }
    }

    private fun Guild.tryRemoveModRole(deleted: Role? = null) {
        val modRole = this.modRole
        if(modRole !== null) {
            if(modRole == deleted) {
                this.modRole = null
            }
        } else {
            if(hasModRole) {
                this.modRole = null
            }
        }
    }

    private fun Guild.tryRemoveMutedRole(deleted: Role? = null) {
        val mutedRole = this.mutedRole
        if(mutedRole !== null) {
            if(mutedRole == deleted) {
                this.mutedRole = null
            }
        } else {
            if(hasMutedRole) {
                this.mutedRole = null
            }
        }
    }
}