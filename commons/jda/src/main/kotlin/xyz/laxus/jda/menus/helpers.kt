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
package xyz.laxus.jda.menus

import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import xyz.laxus.util.functional.AddRemoveBlock

typealias FinalAction = (suspend (Message) -> Unit)
typealias PageFunction<R> = (Int, Int) -> R

internal inline fun <reified M: Menu> M.isValidUser(user: User, guild: Guild? = null): Boolean = when {
    user.isBot -> false
    users.isEmpty() && roles.isEmpty() -> true
    user in users -> true
    guild === null -> false
    else -> guild.getMember(user)?.roles?.any { it in roles } == true
}

class ItemControllerBlock<in T>(private val collection: MutableCollection<T>): AddRemoveBlock<T> {
    override fun add(element: T) {
        collection += element
    }

    override fun remove(element: T) {
        collection -= element
    }
}