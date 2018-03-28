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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")
package xyz.laxus

import net.dv8tion.jda.core.AccountType.BOT
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus.DO_NOT_DISTURB
import net.dv8tion.jda.core.Permission.*
import okhttp3.OkHttpClient
import xyz.laxus.command.Command
import xyz.laxus.jda.ContextEventManager
import xyz.laxus.jda.listeners.EventWaiter
import xyz.laxus.jda.util.*
import xyz.laxus.util.*
import xyz.laxus.util.reflect.packageOf

object Laxus {
    const val DevId = 211393686628597761L
    const val Success = "\uD83D\uDC32"
    const val Warning = "\uD83D\uDC22"
    const val Error = "\uD83D\uDD25"
    const val Prefix = "|"
    const val TestPrefix = "||"
    const val ServerInvite = "https://discord.gg/xkkw54u"
    const val GitHub = "https://github.com/TheMonitorLizard/Laxus"

    val Package = packageOf(Laxus::class)
    val Version = Package.version.implementation ?: "BETA"
    val HttpClientBuilder = OkHttpClient.Builder()
    val Permissions = arrayOf(
        MESSAGE_HISTORY,
        MESSAGE_EMBED_LINKS,
        MESSAGE_ATTACH_FILES,
        MESSAGE_ADD_REACTION,
        MANAGE_PERMISSIONS,
        MANAGE_ROLES,
        MANAGE_CHANNEL,
        NICKNAME_MANAGE,
        MESSAGE_MANAGE,
        KICK_MEMBERS,
        BAN_MEMBERS,
        VIEW_AUDIT_LOGS
    )

    val Log = createLogger(Laxus::class)

    private lateinit var bot: Bot
    private lateinit var jda: JDA
    private lateinit var waiter: EventWaiter
    val Bot get() = bot
    val JDA get() = jda
    val Waiter get() = waiter

    fun start() {
        // First we need a config
        val config = checkNotNull(loadConfig("bot.conf").config("bot")) {
            "Could not find 'bot' node for bot.conf!"
        }

        // Load Command.Groups via reflection
        val groups = config.list("groups")?.mapNotNull groups@ {
            val klass = it.klass ?: run {
                Log.warn("Could not load command group class: '${it.string}")
                return@groups null
            }

            val objectInstance = requireNotNull(klass.objectInstance) {
                "'$klass' is not an object and therefore not a valid command group!"
            }

            return@groups requireNotNull(objectInstance as? Command.Group) {
                "'$klass' is not a subtype of Command.Group and therefore not a valid command group!"
            }
        } ?: emptyList()

        val token = checkNotNull(config.string("token")) {
            "Could not find 'token' node for bot.conf!"
        }

        this.waiter = EventWaiter()

        // Start JDA
        this.jda = jda(BOT) {
            token { token }
            manager { ContextEventManager() }

            groups.forEach { with(it) { configure() } }

            bot {
                this.dBotsKey = config.string("keys.dbots")
                this.dBotsListKey = config.string("keys.dbotslist")

                if(config.boolean("test") == true) {
                    this.prefix = TestPrefix
                }

                config.int("callCacheSize")?.let {
                    this.callCacheSize = it
                }

                config.enum<RunMode>("mode")?.let {
                    this.mode = it
                }

                this.groups += groups.onEach { it.init(config) }.sorted()
            }

            listener { Waiter }

            contextMap { null }

            // Initializing Status
            // This will be overwritten by the Bot's ready listener handle.
            status { DO_NOT_DISTURB }
            watching { "Everything Start Up..." }
        }
    }

    fun stop() {
        if(::jda.isInitialized) {
            JDA.shutdown()
        }
    }

    internal fun initBot(bot: Bot) {
        this.bot = bot
    }
}