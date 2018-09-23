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

inline val User.formattedName: String inline get() = "$name#$discriminator"
inline val User.boldFormattedName: String inline get() = "**$name**#$discriminator"

inline val User.game: Game? inline get() {
    return mutualGuilds.first().getMember(this)?.game
}

inline val User.status: OnlineStatus inline get() {
    return mutualGuilds.first().getMember(this)?.onlineStatus ?: OnlineStatus.OFFLINE
}

inline val User.isSelf: Boolean inline get() {
    return this is SelfUser || jda.selfUser.idLong == idLong
}

inline val Member.connectedChannel: VoiceChannel? inline get() {
    return voiceState.channel
}

inline val Member.isConnected: Boolean inline get() {
    return connectedChannel !== null
}

inline val Member.isAdmin: Boolean inline get() {
    return Permission.ADMINISTRATOR in permissions || isOwner
}
