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
package xyz.laxus.jda.util

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.managers.GuildController
import net.dv8tion.jda.core.managers.RoleManager
import net.dv8tion.jda.core.requests.RestAction
import net.dv8tion.jda.core.requests.restaction.RoleAction
import xyz.laxus.util.functional.AddRemoveBlock
import java.awt.Color

inline fun GuildController.createRole(block: RoleAction.() -> Unit): RoleAction {
    return createRole().apply(block)
}

inline fun Role.edit(init: RoleManager.() -> Unit): RestAction<Void> {
    return manager.apply(init)
}

inline fun RoleAction.name(lazy: () -> String?): RoleAction = setName(lazy())

inline fun RoleAction.color(lazy: () -> Color?): RoleAction = setColor(lazy())

inline fun RoleAction.hoisted(lazy: () -> Boolean): RoleAction = setHoisted(lazy())

inline fun RoleAction.mentionable(lazy: () -> Boolean): RoleAction = setMentionable(lazy())

inline fun RoleAction.permissions(lazy: AddRemoveBlock<Permission>.() -> Unit) {
    val block = object : AddRemoveBlock<Permission> {
        val set = HashSet<Permission>()

        override fun add(element: Permission) {
            set.add(element)
        }

        override fun remove(element: Permission) {
            set.remove(element)
        }
    }

    block.lazy()

    setPermissions(block.set)
}

inline fun RoleManager.name(lazy: () -> String): RoleManager {
    lazy().takeUnless { it.length > 32 || it.isEmpty() }?.let { setName(it) }
    return this
}

inline fun RoleManager.color(lazy: () -> Color?): RoleManager {
    setColor(lazy())
    return this
}

inline fun RoleManager.hoisted(lazy: () -> Boolean): RoleManager {
    setHoisted(lazy())
    return this
}

inline fun RoleManager.mentionable(lazy: () -> Boolean): RoleManager {
    setMentionable(lazy())
    return this
}

inline fun RoleManager.set(lazy: AddRemoveBlock<Permission>.() -> Unit): RoleManager {
    val block = object : AddRemoveBlock<Permission> {
        val give = HashSet<Permission>()
        val revoke = HashSet<Permission>()

        override fun add(element: Permission) {
            if(element in revoke) {
                revoke -= element
            }
            give += element
        }

        override fun remove(element: Permission) {
            if(element in give) {
                give -= element
            }
            revoke += element
        }
    }

    block.lazy()

    givePermissions(block.give)
    revokePermissions(block.revoke)

    return this
}