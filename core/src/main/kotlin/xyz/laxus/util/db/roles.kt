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
@file:Suppress("Unused")
package xyz.laxus.util.db

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import xyz.laxus.db.DBRolePersist
import xyz.laxus.db.DBRoles
import xyz.laxus.db.DBRoles.Type.*

// Mod Role

var Guild.modRole: Role?
    get() = getRoleTypeOf(MODERATOR)
    set(value) = setRoleTypeOf(value, MODERATOR)
val Member.isMod get() = guild.modRole?.let { it in roles } == true
val Guild.hasModRole get() = modRole !== null

// Muted role

var Guild.mutedRole: Role?
    get() = getRoleTypeOf(MUTED)
    set(value) = setRoleTypeOf(value, MUTED)
val Member.isMuted get() = guild.mutedRole?.let { it in roles } == true
val Guild.hasMutedRole get() = mutedRole !== null

// ColorMe

val Guild.colorMeRoles get() = getRolesTypeOf(COLOR_ME)
var Role.isColorMe: Boolean
    get() = DBRoles.isRole(guild.idLong, COLOR_ME)
    set(value) = addOrRemoveRoleTypeOf(value, COLOR_ME)

// RoleMe

val Guild.roleMeRoles get() = getRolesTypeOf(ROLE_ME)
var Role.isRoleMe: Boolean
    get() = DBRoles.isRole(guild.idLong, ROLE_ME)
    set(value) = addOrRemoveRoleTypeOf(value, ROLE_ME)

// Announcements

val Guild.announcementRoles get() = getRolesTypeOf(ANNOUNCEMENTS)
var Role.isAnnouncements: Boolean
    get() = DBRoles.isRole(guild.idLong, ANNOUNCEMENTS)
    set(value) = addOrRemoveRoleTypeOf(value, ANNOUNCEMENTS)

// Ignored

val Guild.ignoredRoles get() = getRolesTypeOf(IGNORED)
var Role.isIgnored: Boolean
    get() = DBRoles.isRole(guild.idLong, IGNORED)
    set(value) = addOrRemoveRoleTypeOf(value, IGNORED)

// Generic

private fun Role.addOrRemoveRoleTypeOf(value: Boolean, type: DBRoles.Type) {
    if(value) {
        DBRoles.addRole(guild.idLong, idLong, type)
    } else {
        DBRoles.removeRole(guild.idLong, idLong, type)
    }
}

private fun Guild.getRoleTypeOf(type: DBRoles.Type): Role? {
    return DBRoles.getRole(idLong, type)?.let { getRoleById(it) }
}

private fun Guild.getRolesTypeOf(type: DBRoles.Type): List<Role> {
    return DBRoles.getRoles(idLong, type).mapNotNull { getRoleById(it) }
}

private fun Guild.setRoleTypeOf(value: Role?, type: DBRoles.Type) {
    if(value !== null) DBRoles.setRole(idLong, value.idLong, type) else DBRoles.removeRole(idLong, type)
}

// Role Persist

val Member.rolePersist: List<Role> get() {
    return DBRolePersist.getRolePersist(guild.idLong, user.idLong).mapNotNull { guild.getRoleById(it) }
}

fun Member.registerRolePersist() {
    DBRolePersist.setRolePersist(guild.idLong, user.idLong, roles.map { it.idLong })
}

fun Member.unregisterRolePersist() {
    DBRolePersist.removeRolePersist(guild.idLong, user.idLong)
}