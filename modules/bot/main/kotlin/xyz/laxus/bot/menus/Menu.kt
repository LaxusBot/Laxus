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
@file:Suppress("UNCHECKED_CAST")
package xyz.laxus.bot.menus

import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import xyz.laxus.bot.listeners.EventWaiter
import xyz.laxus.commons.collections.unmodifiableSet
import java.util.concurrent.TimeUnit

/**
 * Modeled after jagrosh's Menu in JDA-Utilities
 *
 * @author Kaidan Gustave
 */
abstract class Menu(builder: Builder<*,*>) {
    protected val waiter: EventWaiter = builder.waiter
    internal val users: Set<User> = unmodifiableSet(builder.users)
    internal val roles: Set<Role> = unmodifiableSet(builder.roles)
    protected val timeout: Long = builder.timeout
    protected val unit: TimeUnit = builder.unit

    init {
        builder.users.clear()
        builder.roles.clear()
    }

    abstract fun displayIn(channel: MessageChannel)
    abstract fun displayAs(message: Message)

    @Menu.Dsl
    abstract class Builder<B: Builder<B, M>, M: Menu> {
        lateinit var waiter: EventWaiter
        var timeout: Long = -1
        var unit: TimeUnit = TimeUnit.SECONDS
        val users: MutableSet<User> = HashSet()
        val roles: MutableSet<Role> = HashSet()

        operator fun plusAssign(user: User) {
            users.add(user)
        }

        operator fun plus(user: User): B {
            this += user
            return this as B
        }

        operator fun plusAssign(role: Role) {
            roles.add(role)
        }

        operator fun plus(role: Role): B {
            this += role
            return this as B
        }

        @Menu.Dsl
        inline fun waiter(block: () -> EventWaiter): B {
            waiter = block()
            return this as B
        }

        @Menu.Dsl
        inline fun timeout(block: TimeOut.() -> Unit): B {
            TimeOut().apply {
                block()
                this@Builder.timeout = delay
                this@Builder.unit = unit
            }
            return this as B
        }

        @Menu.Dsl
        inline fun role(block: () -> Role): B = plus(block())

        @Menu.Dsl
        inline fun user(block: () -> User): B = plus(block())
    }

    @DslMarker
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
    internal annotation class Dsl
}
