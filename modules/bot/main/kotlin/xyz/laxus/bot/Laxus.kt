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
@file:Suppress("MemberVisibilityCanBePrivate", "unused", "ObjectPropertyName")
package xyz.laxus.bot

import net.dv8tion.jda.core.AccountType.BOT
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission.*
import okhttp3.OkHttpClient
import xyz.laxus.bot.listeners.EventWaiter
import xyz.laxus.bot.mode.RunMode
import xyz.laxus.bot.utils.jda.jda
import xyz.laxus.bot.utils.jda.listener
import xyz.laxus.bot.utils.jda.manager
import xyz.laxus.bot.utils.jda.token
import xyz.laxus.config.*
import xyz.laxus.reflect.packageOf
import xyz.laxus.utils.createLogger
import xyz.laxus.utils.propertyOf

object Laxus {
    const val DevId = 211393686628597761L
    const val Success = "\uD83D\uDC32"
    const val Warning = "\uD83D\uDC22"
    const val Error = "\uD83D\uDD25"
    const val Prefix = "|"
    const val TestPrefix = "||"
    const val ServerInvite = "https://discord.gg/xkkw54u"
    const val GitHub = "https://github.com/LaxusBot/Laxus"

    val Log = createLogger(Laxus::class)
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

    private lateinit var _bot: Bot
    private lateinit var _jda: JDA
    val Bot: Bot get() = _bot
    val JDA: JDA get() = _jda
    val Waiter: EventWaiter get() = TODO()

    @JvmStatic fun start() {
        val config = loadConfig("/bot.conf").config("bot")
        _bot = bot {
            test { config.nullBoolean("test") ?: propertyOf("env.mode") == "test" }
            dBotsKey { config.nullString("keys.dbots") }
            dBotsListKey { config.nullString("keys.dbotslist") }
            mode { config.enum<RunMode>("mode") ?: RunMode.SERVICE }
        }

        _jda = jda(BOT) {
            token { config.string("token") }
            manager { CoroutineEventManager() }
            listener { _bot }
        }
    }
}
