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

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.channel.category.CategoryCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.hooks.EventListener
import xyz.laxus.auto.listener.AutoListener
import xyz.laxus.jda.util.muteRole
import xyz.laxus.jda.util.refreshMutedRole
import xyz.laxus.util.db.*

@AutoListener
class RolesListener {
    fun onReady(event: ReadyEvent) {
        event.jda.guilds.forEach { guild ->
            guild.mutedRole?.let { guild.refreshMutedRole(it) }
        }
    }

    fun onRoleDelete(event: RoleDeleteEvent) {
        val role = event.role

        with(role) {
            // RoleMe Deleted
            if(isRoleMe) {
                isRoleMe = false
            }

            // ColorMe Deleted
            if(isColorMe) {
                isColorMe = false
            }

            // Announcement role
            if(isAnnouncements) {
                isAnnouncements = false
            }

            // Ignored role
            if(isIgnored) {
                isIgnored = false
            }
        }

        val guild = event.guild

        // Mod Role Deleted
        guild.tryRemoveModRole(role)

        // Muted Role Deleted
        guild.tryRemoveMutedRole(role)
    }

    fun onTextChannelCreate(event: TextChannelCreateEvent) {
        event.guild.mutedRole?.let { event.channel.muteRole(it) }
    }

    fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        event.guild.mutedRole?.let { event.channel.muteRole(it) }
    }

    fun onCategoryCreate(event: CategoryCreateEvent) {
        event.guild.mutedRole?.let { event.category.muteRole(it) }
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