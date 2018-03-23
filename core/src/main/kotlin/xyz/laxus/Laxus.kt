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
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission.*
import okhttp3.OkHttpClient
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

    private lateinit var bot: Bot
    private lateinit var jda: JDA
    val Bot get() = bot
    val JDA get() = jda

    fun start() {
        // First we need a config
        val config = checkNotNull(loadConfig("bot.conf").config("bot")) {
            "Could not find 'bot' node for bot.conf!"
        }
        jda = jda(BOT) {
            token {
                checkNotNull(config.string("token")) {
                    "Could not find 'token' node for bot.conf!"
                }
            }

            manager { ContextEventManager() }

            bot {
                this.prefix = TestPrefix.takeIf { config.boolean("test") == true } ?: Prefix
                this.dBotsKey = config.string("keys.dbots")
                this.dBotsListKey = config.string("keys.dbotslist")
                this.callCacheSize = config.int("callCacheSize") ?: 300
                this.mode = config.enum<RunMode>("mode") ?: RunMode.SERVICE
            }

            listener { EventWaiter() }

            contextMap { null }
            status { OnlineStatus.DO_NOT_DISTURB }
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