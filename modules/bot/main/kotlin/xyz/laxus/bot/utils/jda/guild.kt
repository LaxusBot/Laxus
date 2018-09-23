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
@file:Suppress("unused")
package xyz.laxus.bot.utils.jda

import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.*

fun Guild.refreshMutedRole(role: Role) {
    categories.forEach    { it.muteRole(role) }
    textChannels.forEach  { it.muteRole(role) }
    voiceChannels.forEach { it.muteRole(role) }
}

fun Category.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(this, MANAGE_PERMISSIONS))
        return

    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied !== null) {
        val cannotWrite = MESSAGE_WRITE in denied
        val cannotAddReaction = MESSAGE_ADD_REACTION in denied
        val cannotSpeak = VOICE_SPEAK in denied

        if(cannotWrite && cannotAddReaction && cannotSpeak)
            return

        with(overrides.manager) {
            if(!cannotWrite)
                deny(MESSAGE_WRITE)
            if(!cannotAddReaction)
                deny(MESSAGE_ADD_REACTION)
            if(!cannotSpeak)
                deny(VOICE_SPEAK)
            queue()
        }
    } else createPermissionOverride(role).setDeny(MESSAGE_WRITE, MESSAGE_ADD_REACTION, VOICE_SPEAK).queue()
}

fun TextChannel.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(this, MANAGE_PERMISSIONS))
        return

    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied !== null) {
        val cannotWrite = MESSAGE_WRITE in denied
        val cannotAddReaction = MESSAGE_ADD_REACTION in denied

        if(cannotWrite && cannotAddReaction)
            return

        with(overrides.manager) {
            if(!cannotWrite) {
                deny(MESSAGE_WRITE)
            }
            if(!cannotAddReaction) {
                deny(MESSAGE_ADD_REACTION)
            }
            queue()
        }
    } else createPermissionOverride(role).setDeny(MESSAGE_WRITE, MESSAGE_ADD_REACTION).queue()
}

fun VoiceChannel.muteRole(role: Role) {
    if(!guild.selfMember.hasPermission(this, MANAGE_PERMISSIONS))
        return

    val overrides = getPermissionOverride(role)
    val denied = overrides?.denied
    if(denied !== null) {
        if(VOICE_SPEAK in denied)
            return

        overrides.manager.deny(VOICE_SPEAK).queue()
    } else createPermissionOverride(role).setDeny(MESSAGE_WRITE, MESSAGE_ADD_REACTION).queue()
}

infix fun Member.canView(channel: TextChannel): Boolean = hasPermission(channel, MESSAGE_READ)
infix fun Role.canView(channel: TextChannel): Boolean = hasPermission(channel, MESSAGE_READ)
infix fun Member.canJoin(channel: VoiceChannel): Boolean = hasPermission(channel, VOICE_CONNECT)
infix fun Role.canJoin(channel: VoiceChannel): Boolean = hasPermission(channel, VOICE_CONNECT)
