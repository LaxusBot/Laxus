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
@file:Suppress("LeakingThis", "unused", "MemberVisibilityCanBePrivate")
package xyz.laxus.command

import com.typesafe.config.Config
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import xyz.laxus.Bot
import xyz.laxus.Laxus
import xyz.laxus.command.Command.CooldownScope.*
import xyz.laxus.db.stats.DBCommandUsage
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.isAdmin
import xyz.laxus.util.commandArgs
import xyz.laxus.util.db.getCommandLevel
import xyz.laxus.util.db.ignoredRoles
import xyz.laxus.util.db.isIgnored
import xyz.laxus.util.db.isMod
import xyz.laxus.util.ignored
import xyz.laxus.util.modifyIf
import xyz.laxus.util.titleName
import java.util.*
import kotlin.reflect.full.findAnnotation

/**
 * @author Kaidan Gustave
 */
abstract class Command(val group: Command.Group, val parent: Command?): Comparable<Command> {
    companion object {
        const val BotPermError = "${Laxus.Error} I need the %s permission in this %s!"
        const val MissingArguments = "Missing Arguments"
        const val TooManyArguments = "Too Many Arguments"
        const val InvalidArguments = "Invalid Arguments"
        const val UnexpectedError = "An unexpected error occurred, please try again later!"

        private suspend fun sendSubHelp(ctx: CommandContext, command: Command) {
            val helpMessage = buildString {
                val aliases = command.aliases
                val help = command.help
                val arguments = command.arguments
                val experiment = command.experiment
                val children = command.children.filter {
                    it.group.check(ctx) && when {
                        ctx.isPrivate -> it.defaultLevel.test(ctx) && !it.guildOnly
                        it.hasAdjustableLevel -> it.defaultLevel.test(ctx)
                        else -> (ctx.guild.getCommandLevel(it) ?: it.defaultLevel).test(ctx)
                    }
                }

                append("__Available help for **${command.name} Command** in " +
                       "${if(ctx.isPrivate) "DM" else "<#${ctx.channel.id}>"}__\n")

                append("\n**Usage:** `${ctx.bot.prefix}${command.fullname.toLowerCase()}")
                append(if(arguments.isNotEmpty()) " $arguments`" else "`")

                if(aliases.isNotEmpty()) {
                    append("\n**Alias${if(aliases.size > 1) "es" else ""}:** `")
                    for(i in aliases.indices) {
                        append("${aliases[i]}`")
                        if(i != aliases.lastIndex) {
                            append(", `")
                        }
                    }
                }

                if(help != "No help available.") {
                    append("\n**Function:** `$help`\n")
                }

                if(experiment !== null) {
                    append("\n**Experimental:** ")
                    appendln(experiment.info.modifyIf(String::isEmpty) {
                        "This command is experimental." // TODO add info regarding experimental commands
                    })
                }

                if(children.isNotEmpty()) {
                    append("\n**Sub-Commands:**\n\n")
                    var level: Level? = null
                    for((i, c) in children.sorted().withIndex()) {
                        if(level != c.defaultLevel) {
                            if(!c.defaultLevel.test(ctx)) continue
                            level = c.defaultLevel
                            if(level != Level.STANDARD) {
                                if(i != 0) appendln()
                                append("__${level.titleName}__\n\n")
                            }
                        }
                        append("`${ctx.bot.prefix}${c.fullname}")
                        append(if(c.arguments.isNotEmpty()) " ${c.arguments}" else "")
                        append("` - ").append(c.help)

                        if(c.isExperimental) append(" `[EXPERIMENTAL]`")

                        if(i < children.lastIndex) appendln()
                    }
                }

                val owner = ignored(null) { ctx.jda.retrieveUserById(Laxus.DevId).await() }

                if(owner !== null) {
                    append("\n\nFor additional help, contact **")
                    append(owner.name)
                    append("**#")
                    append(owner.discriminator)
                    append(" or join his support server ")
                } else {
                    append("\n\nFor additional help, join my support server ")
                }
                append(Laxus.ServerInvite)
            }

            if(ctx.isGuild) {
                ctx.reactSuccess()
            }

            ctx.replyInDM(helpMessage)
        }
    }

    abstract val name: String

    open val aliases = emptyArray<String>()
    open val arguments = ""
    open val help = "No help available."
    open val devOnly = group.devOnly
    open val guildOnly = group.guildOnly
    open val botPermissions = emptyArray<Permission>()
    open val cooldown = 0
    open val cooldownScope = USER
    open val hasAdjustableLevel = true
    open val children: Array<out Command> = emptyArray()
    open val fullname: String get() = "${parent?.let { "${it.fullname} " } ?: ""}$name"
    open val defaultLevel: Command.Level get() = parent?.defaultLevel ?: group.defaultLevel
    open val isExperimental: Boolean get() = experiment !== null

    private val experiment: Experiment? by lazy { this::class.findAnnotation() ?: parent?.experiment }
    private val autoCooldown by lazy { this::class.findAnnotation<AutoCooldown>()?.mode ?: AutoCooldownMode.OFF }
    private val noArgumentError by lazy {
        val annotation = this::class.findAnnotation<MustHaveArguments>() ?: return@lazy null
        val error = annotation.error
        return@lazy error.replace("%name", fullname).replace("%arguments", arguments)
    }

    constructor(parent: Command): this(parent.group, parent)

    constructor(group: Command.Group): this(group, null)

    suspend fun run(ctx: CommandContext) {
        if(children.isNotEmpty() && ctx.args.isNotEmpty()) {
            val parts = ctx.args.split(commandArgs, 2)
            children.forEach {
                if(it.isForCommand(parts[0])) {
                    ctx.reassignArgs(if(parts.size > 1) parts[1] else "")
                    return it.run(ctx)
                }
            }
        }

        if(devOnly && !ctx.isDev)
            return

        if(isExperimental) {
            // TODO Manage Experiment Beta Permission
        }

        if(guildOnly && !ctx.isGuild)
            return

        if(!group.check(ctx))
            return

        if(ctx.args.startsWith("help", true))
            return sendSubHelp(ctx, this)

        val level = ctx.level

        if(!level.guildOnly || ctx.isGuild) {
            if(!level.test(ctx))
                return
        }

        if(ctx.isGuild) {
            if(ctx.textChannel.isIgnored) {
                if(!Level.MODERATOR.test(ctx))
                    return
            }
            val roles = ctx.member.roles
            if(ctx.guild.ignoredRoles.any { it in roles }) {
                if(!Level.MODERATOR.test(ctx))
                    return
            }

            for(p in botPermissions) {
                if(p.isChannel) {
                    if(p.name.startsWith("VOICE")) {
                        val vc = ctx.member.voiceState.channel
                        if(vc === null) {
                            return ctx.terminate("${Laxus.Error} You must be in a voice channel to use that!")
                        } else if(!ctx.selfMember.hasPermission(vc, p)) {
                            return ctx.terminate(BotPermError.format(p.name, "Voice Channel"))
                        }
                    }
                } else if(!ctx.selfMember.hasPermission(ctx.textChannel, p)) {
                    return ctx.terminate(BotPermError.format(p.name, "Guild"))
                }
            }
        }

        noArgumentError?.let { noArgumentError ->
            if(ctx.args.isEmpty()) {
                if(noArgumentError.isNotEmpty()) {
                    return ctx.terminate(
                        "${Laxus.Error} **$MissingArguments!**\n" +
                        noArgumentError.replace("%prefix", ctx.bot.prefix)
                    )
                }

                return ctx.terminate(
                    "${Laxus.Error} **$MissingArguments!**\n" +
                    "Use `${ctx.bot.prefix}$fullname help` for more info on this command!"
                )
            }
        }

        val key = ctx.takeIf { cooldown > 0 }?.cooldownKey?.also { key ->
            val remaining = ctx.bot.getRemainingCooldown(key)
            if(remaining > 0) {
                val scope = ctx.correctScope
                return ctx.terminate("${Laxus.Warning} That command is on cooldown for $remaining more " +
                                     "seconds${if(scope.errSuffix.isEmpty()) "" else " ${scope.errSuffix}"}!")
            }
        }

        key?.takeIf { autoCooldown == AutoCooldownMode.BEFORE }?.let { ctx.bot.applyCooldown(key, cooldown) }

        try {
            execute(ctx)
        } catch(t: Throwable) {
            val additionalInfo = buildString {
                if(ctx.isGuild) {
                    appendln("Guild: ${ctx.guild.name} (ID: ${ctx.guild.idLong})")
                    appendln("Channel: #${ctx.guild.name} (ID: ${ctx.textChannel.idLong})")
                }
                append("Author: ${ctx.author.let { "${it.name}#${it.discriminator} (ID: ${it.idLong})" }}")
            }
            Bot.error("$fullname encountered an exception:\n$additionalInfo", t)
            return ctx.replyError(UnexpectedError)
        }

        DBCommandUsage.addUsage(
            fullname.toLowerCase(),
            ctx.takeIf { it.isGuild }?.guild?.idLong,
            ctx.channel.idLong, ctx.author.idLong
        )

        key?.takeIf { autoCooldown == AutoCooldownMode.AFTER }?.let { ctx.bot.applyCooldown(key, cooldown) }

        Bot.Log.debug("Completed Command \"$fullname\"")
    }

    protected abstract suspend fun execute(ctx: CommandContext)

    fun isForCommand(string: String): Boolean {
        if(string.equals(name, true)) {
            return true
        }
        aliases.forEach {
            if(string.equals(it, true)) {
                return true
            }
        }
        return false
    }

    fun findChild(args: String): Command? {
        if(children.isEmpty() || args.isEmpty())
            return this

        val parts = args.split(commandArgs, 2)
        children.forEach {
            if(it.isForCommand(parts[0])) {
                return it.findChild(if(parts.size > 1) parts[1] else "")
            }
        }

        return null
    }

    suspend fun uses(guild: Guild? = null): Long {
        return DBCommandUsage.getUsage(fullname.toLowerCase(), guild?.idLong)
    }

    protected val CommandContext.level: Level get() {
        return if(isGuild && hasAdjustableLevel) {
            guild.getCommandLevel(this@Command) ?: defaultLevel
        } else defaultLevel
    }

    protected fun CommandContext.invokeCooldown() = bot.applyCooldown(cooldownKey, cooldown)

    protected inline fun CommandContext.missingArgs(block: () -> String = {
        "See `${bot.prefix}$fullname help` for more information on this command!"
    }) = error(MissingArguments, block)

    protected inline fun CommandContext.invalidArgs(block: () -> String = {
        "See `${bot.prefix}$fullname help` for more information on this command!"
    }) = error(InvalidArguments, block)

    private val CommandContext.cooldownKey: String get() {
        return when(cooldownScope) {
            USER -> cooldownScope.genKey(name, author.idLong)
            USER_GUILD -> {
                if(isGuild) return cooldownScope.genKey(name, author.idLong, guild.idLong)
                return USER_CHANNEL.genKey(name, author.idLong, channel.idLong)
            }
            USER_CHANNEL -> cooldownScope.genKey(name, author.idLong, channel.idLong)
            GUILD -> {
                if(isGuild) return cooldownScope.genKey(name, guild.idLong)
                return CHANNEL.genKey(name, channel.idLong)
            }
            CHANNEL -> cooldownScope.genKey(name, channel.idLong)
            GLOBAL -> cooldownScope.genKey(name, 0L)
        }
    }

    private val CommandContext.correctScope: CooldownScope get() {
        if(!isGuild) {
            return when(cooldownScope) {
                USER_GUILD, GUILD -> CHANNEL
                else -> cooldownScope
            }
        }
        return cooldownScope
    }

    private fun CommandContext.terminate(text: String) {
        reply(text)
        Bot.Log.debug("Terminated Command \"$fullname\" with message: \"$text\"")
    }

    override fun compareTo(other: Command): Int {
        return group.compareTo(other.group).takeIf { it != 0 } ?: fullname.compareTo(other.fullname, true)
    }

    abstract class Group(val name: String): Comparable<Group> {
        abstract val defaultLevel: Level
        abstract val guildOnly: Boolean
        abstract val devOnly: Boolean
        val commands = LinkedList<Command>()

        // Can be used as an arbitrary check for commands under group
        open fun check(ctx: CommandContext): Boolean = true

        // Can be used to configure JDA
        open fun JDABuilder.configure() {}

        open fun dispose() {}

        operator fun Command.unaryPlus() {
            Laxus.Log.debug("Adding Command '${this.name}' to Group '${this@Group.name}'")
            commands += this
        }

        abstract fun init(config: Config)

        override fun compareTo(other: Group): Int {
            if(name == other.name) {
                return 0
            }
            if(name == "Standard") {
                return -1
            }
            if(other.name == "Standard") {
                return 1
            }
            return defaultLevel.compareTo(other.defaultLevel).takeIf { it != 0 }
                   ?: name.compareTo(other.name)
        }
    }

    enum class Level(val guildOnly: Boolean = false, val test: suspend (CommandContext) -> Boolean = { true }) {
        STANDARD(),
        MODERATOR(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isAdmin || ctx.member.isMod }),
        ADMINISTRATOR(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isAdmin }),
        SERVER_OWNER(guildOnly = true, test = { ctx -> ctx.isDev || ctx.member.isOwner }),
        SHENGAERO(test = { ctx -> ctx.isDev });
    }

    enum class CooldownScope constructor(private val format: String, val errSuffix: String) {
        /** `U:(UserID)` */
        USER("U:%d", ""),
        /** `C:(ChannelID)` */
        CHANNEL("C:%d", "in this channel"),
        /** `U:(UserID)|C:(ChannelID)` */
        USER_CHANNEL("U:%d|C:%d", "in this channel"),
        /**
         * `G:(GuildID)`
         *
         * Defaults to [CHANNEL] in DM's
         */
        GUILD("G:%d", "in this server"),
        /**
         * `U:(UserID)|C:(GuildID)`
         *
         * Defaults to [USER_CHANNEL] in DM's
         */
        USER_GUILD("U:%d|G:%d", "in this server"),
        /** `globally` */
        GLOBAL("Global", "globally");

        internal fun genKey(name: String, id: Long) = genKey(name, id, -1)

        internal fun genKey(name: String, idOne: Long, idTwo: Long) = "$name|${when {
            this == GLOBAL -> format
            idTwo == -1L   -> format.format(idOne)
            else           -> format.format(idOne, idTwo)
        }}"
    }
}