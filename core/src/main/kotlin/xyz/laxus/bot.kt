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

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import me.kgustave.json.JSObject
import me.kgustave.json.jsonObject
import me.kgustave.json.readJSObject
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission.MESSAGE_MANAGE
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.requests.Requester.MEDIA_TYPE_JSON
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import org.slf4j.Logger
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.CommandMap
import xyz.laxus.entities.tagMethods
import xyz.laxus.jda.listeners.SuspendedListener
import xyz.laxus.jda.util.listeningTo
import xyz.laxus.logging.LogLevel
import xyz.laxus.logging.NormalFilter
import xyz.laxus.util.*
import xyz.laxus.util.collections.CaseInsensitiveHashMap
import xyz.laxus.util.collections.FixedSizeCache
import xyz.laxus.util.collections.concurrentHashMap
import xyz.laxus.util.collections.sumByLong
import xyz.laxus.util.db.getCustomCommand
import xyz.laxus.util.db.isBlacklisted
import xyz.laxus.util.db.isJoinWhitelisted
import xyz.laxus.util.db.prefixes
import java.io.IOException
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.time.temporal.ChronoUnit.SECONDS
import java.util.*
import java.util.concurrent.TimeUnit.HOURS
import kotlin.coroutines.experimental.coroutineContext

class Bot @PublishedApi internal constructor(builder: Bot.Builder): SuspendedListener {
    companion object Log: Logger by createLogger(Bot::class)

    private val cooldowns = concurrentHashMap<String, OffsetDateTime>()
    private val uses = CaseInsensitiveHashMap<Int>()
    private val callCache = FixedSizeCache<Long, MutableSet<Message>>(builder.callCacheSize)
    private val cycleContext = newSingleThreadContext("CycleContext")

    private val dBotsKey = builder.dBotsKey
    private val dBotsListKey = builder.dBotsListKey

    val test = builder.test
    val prefix = if(test) Laxus.TestPrefix else Laxus.Prefix
    val httpClient: OkHttpClient = Laxus.HttpClientBuilder.build()
    val startTime: OffsetDateTime = now()
    val groups: List<Command.Group> = builder.groups.sorted()
    val commands: Map<String, Command> = CommandMap(*groups.toTypedArray())
    val parser: Parser = JagTag.newDefaultBuilder().addMethods(tagMethods).build()

    val messageCacheSize get() = callCache.size

    var totalGuilds = 0L
        private set
    var mode = builder.mode
        set(value) {
            field = value
            NormalFilter.level = LogLevel.byLevel(field.level)
        }

    init { NormalFilter.level = LogLevel.byLevel(mode.level) }

    fun getRemainingCooldown(name: String): Int {
        cooldowns[name]?.let { cooldown ->
            val time = now().until(cooldown, SECONDS).toInt()
            if(time <= 0) cooldowns -= name else return time
        }
        return 0
    }

    fun applyCooldown(name: String, seconds: Int) {
        cooldowns[name] = now().plusSeconds(seconds.toLong())
    }

    fun cleanCooldowns() {
        val now = now()
        cooldowns.entries.filter { it.value.isBefore(now) }.forEach { cooldowns -= it.key }
    }

    fun incrementUses(command: Command) {
        uses[command.name] = (uses[command.name] ?: 0) + 1
    }

    fun searchCommand(query: String): Command? {
        val splitQuery = query.split(commandArgs, 2)
        if(splitQuery.isEmpty())
            return null
        return commands[splitQuery[0]]?.findChild(if(splitQuery.size > 1) splitQuery[1] else "")
    }

    override suspend fun onEvent(event: Event) {
        when(event) {
            is MessageReceivedEvent -> onMessageReceived(event)
            is ReadyEvent -> onReady(event)
            is GuildJoinEvent -> onGuildJoinEvent(event)
            is GuildLeaveEvent -> onGuildLeaveEvent(event)
            is GuildMessageDeleteEvent -> onGuildMessageDelete(event) // Does not suspend!
            is ShutdownEvent -> onShutdown(event) // Does not suspend!
        }
    }

    private suspend fun onMessageReceived(event: MessageReceivedEvent) {
        // Do not allow bots to trigger any sort of command
        if(event.author.isBot)
            return

        if(event.textChannel?.canTalk() == false)
            return

        val raw = event.message.contentRaw
        val guild = event.guild

        val parts = when {
            raw.startsWith(prefix, true) -> {
                raw.substring(prefix.length).trim().split(commandArgs, 2)
            }

            guild !== null -> {
                val prefixes = event.guild.prefixes

                if(prefixes.isEmpty())
                    return

                val prefix = prefixes.find { raw.startsWith(it, true) } ?: return
                raw.substring(prefix.length).trim().split(commandArgs, 2)
            }

            else -> return
        }

        val name = parts[0].toLowerCase()
        val args = if(parts.size == 2) parts[1] else ""
        if(mode.checkCall(event, this@Bot, name, args)) {
            val ctx = CommandContext(event, args, this@Bot, coroutineContext)
            commands[name]?.let { command ->
                mode.onCommandCall(ctx, command)
                return command.run(ctx)
            }

            if(ctx.isGuild) {
                ctx.guild.getCustomCommand(name)?.let { customCommand ->
                    with(parser) {
                        clear()
                        put("user", event.author)
                        put("guild", event.guild)
                        put("channel", event.textChannel)
                        put("args", args)
                    }

                    ctx.reply(parser.parse(customCommand))
                }
            }
        }
    }

    private suspend fun onReady(event: ReadyEvent) {
        with(event.jda.presence) {
            status = OnlineStatus.ONLINE
            game = listeningTo("type ${prefix}help")
        }

        val si = event.jda.shardInfo
        Log.info("${si?.let { "[${it.shardId} / ${it.shardTotal - 1}]" } ?: "Laxus"} is Online!")

        val toLeave = event.jda.guilds.filter { !it.isGood }
        if(toLeave.isNotEmpty()) {
            toLeave.forEach { it.leave().queue() }
            Log.info("Left ${toLeave.size} bad guilds!")
        }

        // Clear Caches every hour
        if(si === null || si.shardId == 0) {
            launch(cycleContext) {
                while(isActive) {
                    cleanCooldowns()
                    delay(1, HOURS)
                }
            }
        }

        updateStats(event.jda)
    }

    private suspend fun onGuildJoinEvent(event: GuildJoinEvent) {
        if(event.guild.selfMember.joinDate.plusMinutes(10).isAfter(now())) {
            updateStats(event.jda)
        }
    }

    private suspend fun onGuildLeaveEvent(event: GuildLeaveEvent) {
        updateStats(event.jda)
    }

    private fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        val cached = synchronized(callCache) { callCache[event.messageIdLong] } ?: return
        val channel = event.channel
        if(cached.size > 1 && event.guild.selfMember.hasPermission(channel, MESSAGE_MANAGE)) {
            channel.deleteMessages(cached).queue()
        } else if(cached.size < 3) { // Don't try to delete more than two
            cached.forEach { it.delete().queue() }
        }
    }

    private fun onShutdown(event: ShutdownEvent) {
        val identifier = event.jda.shardInfo?.let { "Shard [${it.shardId} / ${it.shardTotal - 1}]" } ?: "JDA"
        Log.info("$identifier has shutdown.")
        cycleContext.close()
    }

    private suspend fun updateStats(jda: JDA) {
        val body = jsonObject { "server_count" to jda.guildCache.size() }

        jda.shardInfo?.let {
            body["shard_id"] = it.shardId
            body["shard_count"] = it.shardTotal
        }

        val bodyString = body.toJsonString(0)
        val requestBody = RequestBody.create(MEDIA_TYPE_JSON, bodyString)

        dBotsKey?.let {
            // Run this as a child job
            launch(coroutineContext) {
                try {
                    val url = "https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats"
                    // Send POST request to bots.discord.pw
                    httpClient.newRequest {
                        post(requestBody).url(url)
                        this["Authorization"] = dBotsKey
                        this["Content-Type"] = "application/json"
                    }.await().close()
                    Log.debug("Sent to Discord Bots: POST - '$url'\n" +
                              bodyString)
                } catch(e: IOException) {
                    Log.error("Failed to send information to bots.discord.pw", e)
                }
            }
        }

        dBotsListKey?.let {
            // Run this as a child job
            launch(coroutineContext) {
                try {
                    val url = "https://discordbots.org/api/bots/${jda.selfUser.id}/stats"
                    // Send POST request to discordbots.org
                    httpClient.newRequest {
                        post(requestBody).url(url)
                        this["Authorization"] = dBotsListKey
                        this["Content-Type"] = "application/json"
                    }.await().close()
                    Log.debug("Sent to Discord Bots List: POST - '$url'\n" +
                              bodyString)
                } catch(e: IOException) {
                    Log.error("Failed to send information to discordbots.org", e)
                }
            }
        }

        // If we're not sharded there's no reason to send a GET request
        if(jda.shardInfo === null || dBotsKey === null) {
            this.totalGuilds = jda.guildCache.size()
            return
        }

        try {
            // Send GET request to bots.discord.pw
            val url = "https://bots.discord.pw/api/bots/${jda.selfUser.id}/stats"
            httpClient.newRequest {
                get().url(url)
                this["Authorization"] = dBotsKey
                this["Content-Type"] = "application/json"
            }.await().body()?.charStream()?.use {
                val json = it.readJSObject()
                Log.debug("Received JSON from bots.discord.pw:\n${json.toJsonString(2)}")
                this.totalGuilds = json.array("stats").mapNotNull {
                    val obj = it as? JSObject
                    obj?.takeIf { "server_count" in obj && !obj.isNull("server_count") }
                }.sumByLong { it.int("server_count").toLong() }
                Log.debug("Got from Discord Bots: GET - '$url'\n" +
                          json.toJsonString(0))
            }
        } catch (t: Throwable) {
            Log.error("Failed to retrieve bot shard information from bots.discord.pw", t)
        }
    }

    private val Guild.isGood: Boolean get() {
        if(isBlacklisted)
            return false
        if(isJoinWhitelisted)
            return true
        return members.count { it.user.isBot } <= 30 || getMemberById(Laxus.DevId) !== null
    }

    internal fun linkCall(id: Long, message: Message) {
        if(!message.channelType.isGuild) return
        callCache.computeIfAbsent(id) { HashSet() } += message
    }

    interface Listener {
        fun checkCall(event: MessageReceivedEvent, bot: Bot, name: String, args: String): Boolean = true
        fun onCommandCall(ctx: CommandContext, command: Command) {}
        fun onCommandTerminated(ctx: CommandContext, command: Command, msg: String) { ctx.reply(msg) }
        fun onCommandCompleted(ctx: CommandContext, command: Command) {}
        fun onException(ctx: CommandContext, command: Command, exception: Throwable) {
            Log.error("${command.fullname} encountered an exception:", exception)
        }
    }

    class Builder internal constructor() {
        val groups = LinkedList<Command.Group>()
        var test = false
        var dBotsKey: String? = null
        var dBotsListKey: String? = null
        var callCacheSize = 300
        var mode = RunMode.SERVICE
    }
}

internal inline fun bot(build: Bot.Builder.() -> Unit): Bot = Bot(Bot.Builder().apply(build))