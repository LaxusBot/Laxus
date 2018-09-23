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
@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.laxus.bot

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.HttpPlainText
import kotlinx.coroutines.*
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ReadyEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.slf4j.event.Level
import xyz.laxus.bot.command.Command
import xyz.laxus.bot.command.CommandContext
import xyz.laxus.bot.command.CommandGroup
import xyz.laxus.bot.entities.TagErrorException
import xyz.laxus.bot.entities.tagMethods
import xyz.laxus.bot.listeners.SuspendListener
import xyz.laxus.bot.mode.BotMode
import xyz.laxus.bot.mode.RunMode
import xyz.laxus.bot.requests.dbots.DiscordBotsError
import xyz.laxus.bot.requests.dbots.DiscordBotsRequester
import xyz.laxus.bot.utils.commandArgsOf
import xyz.laxus.bot.requests.ktor.AuthorizationHeaders
import xyz.laxus.bot.requests.ktor.ClientCallLogging
import xyz.laxus.bot.requests.ktor.logging
import xyz.laxus.bot.utils.db.*
import xyz.laxus.bot.utils.jda.listeningTo
import xyz.laxus.commons.collections.CaseInsensitiveHashMap
import xyz.laxus.commons.collections.FixedSizeCache
import xyz.laxus.commons.collections.concurrentHashMap
import xyz.laxus.commons.collections.sumByLong
import xyz.laxus.utils.createLogger
import java.time.OffsetDateTime
import java.time.OffsetDateTime.now
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@DslMarker
@Retention(AnnotationRetention.SOURCE)
private annotation class BotBuilderDsl

@BotBuilderDsl internal inline fun bot(build: Bot.Builder.() -> Unit) = Bot(Bot.Builder().apply(build))

class Bot internal constructor(builder: Bot.Builder): SuspendListener {
    private val cooldowns = concurrentHashMap<String, OffsetDateTime>()
    private val uses = CaseInsensitiveHashMap<Int>()
    private val callCache = FixedSizeCache<Long, MutableSet<Message>>(builder.callCacheSize)

    private val botsListsContext = newSingleThreadContext("BotsLists Context")
    private val cycleContext = newSingleThreadContext("Cycle Context")

    private lateinit var reminders: ReminderManager
    private var _totalGuilds = 0L

    val parser: Parser = JagTag.newDefaultBuilder().addMethods(tagMethods).build()
    val prefix: String = Laxus.Prefix.takeIf { !builder.test } ?: Laxus.TestPrefix
    val startTime: OffsetDateTime = now()
    val groups = builder.groups.sorted()
    val commands = mapCommandNames(groups.fold(arrayListOf()) { l, g -> l.apply { addAll(g.commands) } })

    val messageCacheSize get() = callCache.size
    val totalGuilds get() = _totalGuilds
    var mode: BotMode = builder.mode
        set(value) {
            val previous = field
            field = value
            previous.onDetach(this)
            value.onAttach(this)
        }

    val httpClient = HttpClient(OkHttp) {
        install(HttpPlainText) {
            defaultCharset = Charsets.UTF_8
        }

        install(AuthorizationHeaders) {
            resolver(host = "discordbots.org", authorization = builder.dBotsListKey)
        }

        logging {
            name("BotLists")
            send {
                level = Level.INFO
                mode = ClientCallLogging.Mode.SIMPLE
            }
            receive {
                level = Level.DEBUG
                mode = ClientCallLogging.Mode.DETAILED
            }
            error {
                level = Level.ERROR
                mode = ClientCallLogging.Mode.SIMPLE
            }
        }
    }

    val discordBots = DiscordBotsRequester(httpClient, builder.dBotsKey)

    fun getRemainingCooldown(name: String): Int {
        cooldowns[name]?.let { cooldown ->
            val time = now().until(cooldown, ChronoUnit.SECONDS).toInt()
            if(time <= 0) cooldowns -= name else return time
        }
        return 0
    }

    fun applyCooldown(name: String, seconds: Long) {
        cooldowns[name] = now().plusSeconds(seconds)
    }

    fun cleanCooldowns() {
        val now = now()
        cooldowns.entries.filter { it.value.isBefore(now) }.forEach { cooldowns -= it.key }
    }

    fun getUses(command: Command): Int {
        return uses.computeIfAbsent(command.name) { 0 }
    }

    fun incrementUses(command: Command) {
        uses[command.name] = (uses[command.name] ?: 0) + 1
    }

    fun searchCommand(query: String): Command? {
        val splitQuery = commandArgsOf(query, limit = 2)
        if(splitQuery.isEmpty())
            return null
        return commands[splitQuery[0]]?.findChild(if(splitQuery.size > 1) splitQuery[1] else "")
    }

    override suspend fun onEvent(event: Event) {
        when(event) {
            is MessageReceivedEvent -> handleMessage(event)
            is GuildMemberJoinEvent -> handleWelcome(event)
            is ReadyEvent           -> handleReady(event)
        }
    }

    private fun handleReady(event: ReadyEvent) {
        with(event.jda.presence) {
            status = OnlineStatus.ONLINE
            game = listeningTo("type ${prefix}help")
        }

        val shardInfo = event.jda.shardInfo
        Log.info("${shardInfo?.let { "[${it.shardId} / ${it.shardTotal - 1}]" } ?: "Laxus"} is Online!")

        val toLeave = event.jda.guildCache.filter { !it.isGood }

        if(toLeave.isNotEmpty()) {
            toLeave.forEach { it.leave().queue() }
            Log.info("Left ${toLeave.size} bad guilds!")
        }

        // Clear Caches every hour
        if(shardInfo === null || shardInfo.shardId == 0) {
            GlobalScope.launch(cycleContext) {
                while(isActive) {
                    cleanCooldowns()
                    delay(1, TimeUnit.HOURS)
                }
            }
        }
    }

    private suspend fun handleMessage(event: MessageReceivedEvent) {
        // Do not allow bots to trigger any sort of command
        if(event.author.isBot) return
        if(event.textChannel?.canTalk() == false) return
        val raw = event.message.contentRaw
        val guild = event.guild
        val parts = when {
            raw.startsWith(prefix, true) -> {
                commandArgsOf(raw.substring(prefix.length), limit = 2)
            }

            guild !== null -> {
                val prefixes = event.guild.prefixes
                val prefix = prefixes.find { raw.startsWith(it, true) } ?: return
                commandArgsOf(raw.substring(prefix.length), limit = 2)
            }

            else -> return
        }

        val name = parts[0].toLowerCase()
        val args = if(parts.size == 2) parts[1] else ""

        if(!mode.interceptCall(event, this, name, args)) return

        val ctx = CommandContext(event, args, this)

        commands[name]?.let { command ->
            Bot.Log.debug("Call to Command \"${command.fullname}\"")
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

                val parsed = try {
                    parser.parse(customCommand)
                } catch(e: TagErrorException) {
                    return ctx.replyError {
                        e.message ?: "Custom command \"$name\" could not be processed for an unknown reason!"
                    }
                }

                ctx.reply(parsed)
            }
        }
    }

    private fun handleWelcome(event: GuildMemberJoinEvent) {
        val guild = event.guild
        // If there's no welcome then we just return.
        val welcome = guild.welcome ?: return
        // We can't even send messages to the channel so we return
        if(!welcome.first.canTalk()) return
        // We prevent possible spam by creating a cooldown key 'welcomes|U:<User ID>|G:<Guild ID>'
        val cooldownKey = "welcomes|U:${event.user.idLong}|G:${guild.idLong}"
        val remaining = getRemainingCooldown(cooldownKey)
        // Still on cooldown - we're done here
        if(remaining > 0) return
        val message = parser.clear()
            .put("guild", guild)
            .put("channel", welcome.first)
            .put("user", event.user)
            .parse(welcome.second)
        // Too long or empty means we can't send, so we just return because it'll break otherwise
        if(message.isEmpty() || message.length > 2000) return
        // Send Message
        welcome.first.sendMessage(message).queue()
        // Apply cooldown
        applyCooldown(cooldownKey, 100)
    }

    internal fun linkCall(id: Long, message: Message) {
        if(!message.isFromType(ChannelType.TEXT)) return
        Log.debug("Linking response (ID: ${message.idLong}) to call message ID: $id")
        callCache.computeIfAbsent(id, { hashSetOf() }) += message
    }

    private suspend fun updateStats(jda: JDA) {
        GlobalScope.launch(botsListsContext) {
            runCatching { discordBots.postStats(jda) }.getOrElse {
                when(it) {
                    !is DiscordBotsError -> Log.warn("Failed to post to bots.discord.pw")
                    else -> Log.warn("Failed to post to bots.discord.pw - ${it.code}")
                }
            }
        }

        if(jda.shardInfo === null) return

        GlobalScope.launch(botsListsContext) {
            _totalGuilds = runCatching { discordBots.getStats(jda) }.getOrElse {
                return@launch when(it) {
                    !is DiscordBotsError -> Log.warn("Failed to post to bots.discord.pw")
                    else -> Log.warn("Failed to post to bots.discord.pw - ${it.code}")
                }
            }.stats.sumByLong { it.serverCount }
        }
    }

    private val Guild.isGood: Boolean get() {
        if(isBlacklisted)
            return false
        if(isJoinWhitelisted)
            return true
        return members.count { it.user.isBot } <= 30 || getMemberById(Laxus.DevId) !== null
    }

    companion object {
        val Log = createLogger(Bot::class)
    }

    class Builder internal constructor() {
        val groups = arrayListOf<CommandGroup>()

        var test = false
        var dBotsKey: String? = null
        var dBotsListKey: String? = null
        var callCacheSize = 300
        var mode = RunMode.SERVICE

        @BotBuilderDsl inline fun group(block: () -> CommandGroup) {
            groups += block()
        }

        @BotBuilderDsl inline fun test(block: () -> Boolean) {
            test = block()
        }

        @BotBuilderDsl inline fun dBotsKey(block: () -> String?) {
            dBotsKey = block()
        }

        @BotBuilderDsl inline fun dBotsListKey(block: () -> String?) {
            dBotsListKey = block()
        }

        @BotBuilderDsl inline fun callCacheSize(block: () -> Int) {
            callCacheSize = block()
        }

        @BotBuilderDsl inline fun mode(block: () -> RunMode) {
            mode = block()
        }
    }
}

private fun mapCommandNames(commands: List<Command>): Map<String, Command> {
    val map = hashMapOf<String, Command>()
    for(command in commands) {
        require(command.name !in map)
        map[command.name] = command
        for(alias in command.aliases) {
            require(alias !in map)
            map[alias] = command
        }
    }
    return map
}
