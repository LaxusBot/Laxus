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
package xyz.laxus.jda.util

import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.requests.restaction.AuditableRestAction

inline fun <reified M: Member> M.kick(reason: String? = null): AuditableRestAction<Void> {
    return guild.controller.kick(this, reason)
}

inline fun <reified M: Member> M.giveRole(role: Role): AuditableRestAction<Void> {
    return guild.controller.addRolesToMember(this, role)
}

inline fun <reified M: Member> M.removeRole(role: Role): AuditableRestAction<Void> {
    return guild.controller.removeRolesFromMember(this, role)
}

inline fun <reified U: User> U.banFrom(guild: Guild, delDays: Int, reason: String? = null): AuditableRestAction<Void> {
    return guild.controller.ban(this, delDays, reason)
}

inline fun <reified U: User> U.unbanFrom(guild: Guild): AuditableRestAction<Void> {
    return guild.controller.unban(this)
}

inline val <reified U: User> U.game: Game? inline get() {
    return mutualGuilds.first().getMember(this)?.game
}

inline val <reified U: User> U.status: OnlineStatus inline get() {
    return mutualGuilds.first().getMember(this)?.onlineStatus ?: OnlineStatus.OFFLINE
}

inline val <reified U: User> U.isSelf: Boolean inline get() {
    return this is SelfUser || jda.selfUser.idLong == idLong
}

inline val <reified M: Member> M.connectedChannel: VoiceChannel? inline get() {
    return voiceState.channel
}

inline val <reified M: Member> M.isConnected: Boolean inline get() {
    return connectedChannel !== null
}

inline val <reified M: Member> M.isAdmin: Boolean inline get() {
    return Permission.ADMINISTRATOR in permissions || isOwner
}
