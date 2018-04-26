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
package xyz.laxus.listeners

import kotlinx.coroutines.experimental.*
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.audit.AuditLogEntry
import net.dv8tion.jda.core.audit.AuditLogKey
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.GenericGuildEvent
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import xyz.laxus.db.entities.Case
import xyz.laxus.db.entities.Case.Action.*
import xyz.laxus.jda.listeners.SuspendedListener
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.isSelf
import xyz.laxus.util.createLogger
import xyz.laxus.util.db.*
import xyz.laxus.util.formattedName
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.coroutineContext

/**
 * @author Kaidan Gustave
 */
object ModLog: SuspendedListener {
    private const val ReasonLineFormat = "`[ REASON ]` %s"
    private const val FullFormat = "`[  CASE  ]` `[%d]` %s %s %s **%s** (ID: %d)\n$ReasonLineFormat"
    private const val DefaultReason = "none"
    private const val RetrieveAmount = 20

    @Volatile private var running = false

    private val context by lazy { newSingleThreadContext("ModLog-Context") }
    private val eventCache = ConcurrentLinkedQueue<GenericGuildEvent>()
    private val auditCache = ConcurrentHashMap<Long, List<AuditLogEntry>>()

    val Log = createLogger(ModLog::class)

    override suspend fun onEvent(event: Event) {
        if(event is ShutdownEvent) {
            // Time to shut down
            running = false
            eventCache.clear()
            auditCache.clear()
            context.cancel(CancellationException("Shutdown Event was fired"))
            context.close()
            return
        }

        if(event !is GenericGuildEvent) return

        val guild = when(event) {
            is GuildBanEvent -> event.guild
            is GuildUnbanEvent -> event.guild
            is GuildMemberLeaveEvent -> event.guild
            is GuildMemberRoleAddEvent -> {
                event.guild.takeIf { it.mutedRole?.let { it in event.roles } == true }
            }
            is GuildMemberRoleRemoveEvent -> {
                event.guild.takeIf { it.mutedRole?.let { it in event.roles } == true }
            }

            else -> null
        }

        if(guild === null || !guild.hasModLog)
            return

        if(guild.modLog?.canTalk().let { it == null || it == false })
            return

        launch(coroutineContext) {
            // We need to wait a second in case the audit
            // log is slow to update.
            delay(1, TimeUnit.SECONDS)
            Log.debug("Got moderator event to log...")
            eventCache += event
            start()
        }
    }

    suspend fun newBan(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, BAN, reason)
    }

    suspend fun newUnban(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, UNBAN, reason)
    }

    suspend fun newKick(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, KICK, reason)
    }

    suspend fun newMute(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, MUTE, reason)
    }

    suspend fun newUnmute(moderator: Member, target: User, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, UNMUTE, reason)
    }

    suspend fun newClean(moderator: Member, target: TextChannel, msgs: Int, reason: String? = null) {
        val guild = moderator.guild
        val modLog = guild.modLog ?: return
        log(modLog, target, moderator.user, CLEAN, reason, msgs)
    }

    suspend fun editReason(logMessage: Message, reason: String) {
        require(logMessage.author.isSelf) { "ModLog message provided was from a different user!" }

        val parts = logMessage.contentRaw.split('\n', limit = 2)

        require(parts.isNotEmpty()) { "ModLog message when split was an empty collection!" }

        val newContent = "${parts[0]}\n${ReasonLineFormat.format(reason)}"

        logMessage.editMessage(newContent).await()
    }

    private fun start() {
        if(running) return
        running = true
        launch(context) {
            Log.debug("Starting new cache clear (Playing ${eventCache.size} events)...")
            try {
                while(running) {
                    // Wait here and check every two seconds until the eventCache is not empty
                    while(eventCache.isEmpty()) {
                        delay(2, TimeUnit.SECONDS)
                    }

                    while(eventCache.isNotEmpty()) try {
                        val event = eventCache.poll() ?: continue
                        val entries = pollLogOrGetFromCache(event.guild)
                        when(event) {
                            is GuildBanEvent -> onBan(event, entries)
                            is GuildUnbanEvent -> onUnban(event, entries)
                            is GuildMemberLeaveEvent -> onLeave(event, entries)
                            is GuildMemberRoleAddEvent -> onRoleAdd(event, entries)
                            is GuildMemberRoleRemoveEvent -> onRoleRemove(event, entries)
                            else -> Log.warn("Event cache polled was an invalid type: ${event::class}")
                        }
                    } catch(t: Throwable) {
                        if(t is CancellationException) throw t
                    } finally {
                        // Always clear auditCache after a pass
                        Log.debug("Clearing audit cache...")
                        auditCache.clear()
                    }
                }
            } catch(cancellation: CancellationException) {
                Log.debug("Encountered cancellation exception while running ModLog process: ${cancellation.message}")
            }
        }.invokeOnCompletion {
            Log.debug("ModLog process ended.")
            it?.let { Log.warn("ModLog process ended due to an exception", it) }

            auditCache.clear()
            running = false
        }
    }

    private suspend fun onBan(event: GuildBanEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val target = event.user
        val entry = entries.firstOrNull { it.type == ActionType.BAN && it.targetIdLong == target.idLong }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            return Log.warn("Could not find ban for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return
        val reason = entry.reason ?: DefaultReason

        log(modLog, target, moderator, BAN, reason)
    }

    private suspend fun onUnban(event: GuildUnbanEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val target = event.user
        val entry = entries.firstOrNull { it.type == ActionType.UNBAN && it.targetIdLong == target.idLong }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            return Log.warn("Could not find unban for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return
        val reason = entry.reason ?: DefaultReason

        log(modLog, target, moderator, UNBAN, reason)
    }

    private suspend fun onLeave(event: GuildMemberLeaveEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val target = event.user

        // We are not going to log multiple manual kicks by the same user
        // my reasoning for this is because I'd rather lean towards when
        // deciding when not to log than when to log
        val entry = entries.singleOrNull { it.type == ActionType.KICK && it.targetIdLong == target.idLong }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            // This will happen more often than not, and thus
            // we don't log it at warn level
            return Log.debug("Could not find kick for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return
        val reason = entry.reason ?: DefaultReason

        log(modLog, target, moderator, KICK, reason)
    }

    private suspend fun onRoleAdd(event: GuildMemberRoleAddEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val mutedRole = guild.mutedRole ?: return
        val target = event.user
        val entry = entries.firstOrNull entry@ {
            if(it.type == ActionType.MEMBER_ROLE_UPDATE && it.targetIdLong == target.idLong) {
                val change = it.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD)
                if(change === null)
                    return@entry false

                return@entry change.getNewValue<List<Map<String, String>>>().any { it["name"] == mutedRole.name }
            }

            return@entry false
        }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            return Log.warn("Could not find role update for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return

        log(modLog, target, moderator, MUTE, null)
    }

    private suspend fun onRoleRemove(event: GuildMemberRoleRemoveEvent, entries: List<AuditLogEntry>) {
        val guild = event.guild
        val modLog = guild.modLog ?: return
        val mutedRole = guild.mutedRole ?: return
        val target = event.user
        val entry = entries.firstOrNull entry@ {
            if(it.type == ActionType.MEMBER_ROLE_UPDATE && it.targetIdLong == target.idLong) {
                val change = it.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE)
                if(change === null)
                    return@entry false

                return@entry change.getNewValue<List<Map<String, String>>>().any { it["name"] == mutedRole.name }
            }

            return@entry false
        }

        // None found, this pass was a failure unfortunately.
        if(entry === null) {
            return Log.warn("Could not find role update for target (ID: ${event.user.idLong})")
        }

        val moderator = entry.user.takeIf { !it.isSelf } ?: return

        log(modLog, target, moderator, UNMUTE, null)
    }

    private suspend fun log(modLog: TextChannel, target: ISnowflake, moderator: User,
                            action: Case.Action, reason: String?, argument: Any? = null) {
        val guild = modLog.guild
        val number = guild.currentCaseNumber

        val text = FullFormat.format(
            number,
            action.emote(),
            moderator.formattedName(true),
            action.keyword(argument),
            (target as? User)?.name ?: (target as? TextChannel)?.asMention ?: "UNKNOWN",
            target.idLong,
            reason ?: DefaultReason
        )

        Log.debug("Sending case to Moderator Log (ID: ${modLog.idLong}, Guild ID: ${guild.idLong})")
        val message = try {
            modLog.sendMessage(text).await()
        } catch(t: Throwable) {
            return Log.error("An error occurred while sending a case to a ModLog!", t)
        }

        val case = Case(
            number = number,
            guildId = guild.idLong,
            messageId = message.idLong,
            modId = moderator.idLong,
            targetId = target.idLong,
            isOnUser = target is User,
            action = action,
            reason = reason
        )

        guild.addCase(case)
    }

    private suspend fun pollLogOrGetFromCache(guild: Guild): List<AuditLogEntry> {
        return auditCache[guild.idLong] ?: run {
            Log.debug("Polling AuditLog for Guild ID: ${guild.idLong} (max $RetrieveAmount entries)")
            guild.auditLogs.limit(RetrieveAmount).await().also {
                auditCache[guild.idLong] = it
            }
        }
    }

    private fun Case.Action.keyword(argument: Any? = null): String {
        return when(this) {
            BAN -> "banned"
            KICK -> "kicked"
            UNBAN -> "unbanned"
            MUTE -> "muted"
            UNMUTE -> "unmuted"
            CLEAN -> "cleaned %d messages in".format(argument)
            OTHER -> ""
        }
    }

    private fun Case.Action.emote(): String {
        return when(this) {
            BAN -> "\uD83D\uDD28"
            KICK -> "\uD83D\uDC62"
            MUTE -> "\uD83D\uDD07"
            UNMUTE -> "\uD83D\uDD08"
            else -> ""
        }
    }
}