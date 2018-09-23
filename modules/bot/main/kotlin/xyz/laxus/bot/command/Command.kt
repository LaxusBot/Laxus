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
@file:Suppress("LeakingThis", "unused", "MemberVisibilityCanBePrivate", "CanBeParameter")
package xyz.laxus.bot.command

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.User
import xyz.laxus.bot.Bot
import xyz.laxus.bot.Laxus
import xyz.laxus.bot.command.CooldownScope.*
import xyz.laxus.bot.utils.commandArgsOf
import xyz.laxus.bot.utils.db.getCommandLevel
import xyz.laxus.bot.utils.db.ignoredRoles
import xyz.laxus.bot.utils.db.isIgnored
import xyz.laxus.bot.utils.jda.await
import xyz.laxus.utils.titleName
import kotlin.reflect.full.findAnnotation

abstract class Command(
    val group: CommandGroup,
    val parent: Command? = null
): Comparable<Command> {
    constructor(parent: Command): this(parent.group, parent)

    /////////////////////////
    // Required Properties //
    /////////////////////////

    abstract val name: String

    /////////////////////////
    // Optional Properties //
    /////////////////////////

    open val aliases = emptyArray<String>()
    open val arguments = ""
    open val help = "No help available."
    open val devOnly = group.devOnly
    open val guildOnly = group.guildOnly
    open val botPermissions = emptyArray<Permission>()
    open val cooldown: Int = 0
    open val cooldownScope: CooldownScope = USER
    open val hasAdjustableLevel: Boolean = true
    open val children: Array<out Command> = emptyArray()
    open val fullname: String = parent?.fullname?.let { "$it $name" } ?: name
    open val defaultLevel: CommandLevel = parent?.defaultLevel ?: group.defaultLevel
    open val unlisted = group.unlisted

    ////////////////////////
    // Private Properties //
    ////////////////////////

    private val autoCooldown by lazy {
        return@lazy this::class.findAnnotation<AutoCooldown>()?.mode
    }

    private val noArgumentError by lazy {
        return@lazy this::class.findAnnotation<MustHaveArguments>()?.error
            ?.replace("%name", fullname)
            ?.replace("%arguments", arguments)
    }

    ///////////////////////////
    // Implemented Functions //
    ///////////////////////////

    protected abstract suspend fun execute(ctx: CommandContext)

    suspend fun run(ctx: CommandContext) {
        if(children.isNotEmpty() && ctx.args.isNotEmpty()) {
            val parts = commandArgsOf(ctx.args, limit = 2)
            for(child in children) {
                if(child.isForCommand(parts[0])) {
                    ctx.reassignArgs(parts.getOrNull(1) ?: "")
                    return child.run(ctx)
                }
            }
        }

        // at this point, the lowest matching child is finally being "called"
        ctx.bot.mode.onCommandCall(ctx, this)

        if(devOnly && !ctx.isDev)
            return

        if(guildOnly && !ctx.isGuild)
            return

        if(!group.check(ctx))
            return

        if(!unlisted && ctx.args.startsWith("help", true))
            return sendSubHelp(ctx, this)

        val level = ctx.level

        if(!level.guildOnly || ctx.isGuild) {
            if(!level.test(ctx))
                return
        }

        if(ctx.isGuild) {
            if(ctx.textChannel.isIgnored) {
                if(!CommandLevel.MODERATOR.test(ctx))
                    return
            }
            val roles = ctx.member.roles
            if(ctx.guild.ignoredRoles.any { it in roles }) {
                if(!CommandLevel.MODERATOR.test(ctx))
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

        key?.takeIf { autoCooldown == AutoCooldown.Mode.BEFORE }?.let {
            ctx.bot.applyCooldown(key, cooldown.toLong())
        }

        try {
            execute(ctx)
        } catch(cex: CommandError) {
            ctx.bot.mode.onCommandError(ctx, this, cex)
            return ctx.replyError(cex.message)
        } catch(t: Throwable) {
            val additionalInfo = buildString {
                if(ctx.isGuild) {
                    appendln("Guild: ${ctx.guild.name} (ID: ${ctx.guild.idLong})")
                    appendln("Channel: #${ctx.textChannel.name} (ID: ${ctx.textChannel.idLong})")
                }
                append("Author: ${ctx.author.let { "${it.name}#${it.discriminator} (ID: ${it.idLong})" }}")
            }
            Bot.Log.error("$fullname encountered an exception:\n$additionalInfo", t)
            return ctx.replyError(UnexpectedError)
        }

        key?.takeIf { autoCooldown == AutoCooldown.Mode.AFTER }?.let {
            ctx.bot.applyCooldown(key, cooldown.toLong())
        }

        ctx.bot.incrementUses(this)
        ctx.bot.mode.onCommandComplete(ctx, this)
    }

    //////////////////////
    // Helper Functions //
    //////////////////////

    fun isForCommand(string: String): Boolean {
        if(string.equals(name, true)) return true
        return aliases.any { string.equals(it, true) }
    }

    fun findChild(args: String): Command? {
        if(children.isEmpty() || args.isEmpty()) return this

        val parts = commandArgsOf(args, limit = 2)
        return children.firstOrNull { it.isForCommand(parts[0]) }
            ?.findChild(if(parts.size > 1) parts[1] else "")
    }

    /////////////////////////////////////////////
    // Protected Extensions for CommandContext //
    /////////////////////////////////////////////

    protected val CommandContext.level: CommandLevel get() {
        return if(!(isGuild && hasAdjustableLevel)) defaultLevel
        else guild.getCommandLevel(this@Command) ?: defaultLevel
    }

    protected fun CommandContext.invokeCooldown() = bot.applyCooldown(cooldownKey, cooldown.toLong())

    protected inline fun CommandContext.missingArgs(block: () -> String = {
        "See `${bot.prefix}$fullname help` for more information on this command!"
    }): Nothing = error(MissingArguments, block)

    protected inline fun CommandContext.invalidArgs(block: () -> String = {
        "See `${bot.prefix}$fullname help` for more information on this command!"
    }): Nothing = error(InvalidArguments, block)

    ///////////////////////////////////////////
    // Private Extensions for CommandContext //
    ///////////////////////////////////////////

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
        bot.mode.onCommandTerminated(this, this@Command, text)
        Bot.Log.debug("Terminated Command \"$fullname\" with message: \"$text\"")
    }

    ////////////////////////
    // Override Functions //
    ////////////////////////

    final override fun compareTo(other: Command): Int {
        return group.compareTo(other.group).takeIf { it != 0 } ?: fullname.compareTo(other.fullname, true)
    }

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
                val children = command.children.filter {
                    !it.unlisted && it.group.check(ctx) && when {
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

                if(children.isNotEmpty()) {
                    append("\n**Sub-Commands:**\n\n")
                    var level = null as CommandLevel?
                    for((i, c) in children.asSequence().sorted().withIndex()) {
                        if(level != c.defaultLevel) {
                            if(!c.defaultLevel.test(ctx)) continue
                            level = c.defaultLevel
                            if(level != CommandLevel.PUBLIC) {
                                if(i != 0) appendln()
                                append("__${level.titleName}__\n\n")
                            }
                        }
                        append("`${ctx.bot.prefix}${c.fullname}")
                        append(if(c.arguments.isNotEmpty()) " ${c.arguments}" else "")
                        append("` - ").append(c.help)

                        if(i < children.lastIndex) appendln()
                    }
                }

                val owner = runCatching<User?> {
                    ctx.jda.retrieveUserById(Laxus.DevId).await()
                }.getOrNull()

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
}
