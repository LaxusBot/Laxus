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
package xyz.laxus.command.moderator

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import xyz.laxus.Laxus
import xyz.laxus.command.Command
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.command.CommandContext
import xyz.laxus.jda.util.await
import xyz.laxus.listeners.ModLog
import xyz.laxus.jda.util.producePast
import xyz.laxus.util.discordID
import xyz.laxus.util.reasonPattern
import xyz.laxus.util.userMention
import java.util.*
import kotlin.coroutines.experimental.coroutineContext

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class CleanCommand: Command(ModeratorGroup) {
    private companion object {
        private const val MaxRetrievable = 100
        private val numberPattern = Regex("(\\d{1,4})")
        private val linkPattern = Regex("https?://\\S+")
        private val quotePattern = Regex("\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)

        private val Message.hasImage get() = attachments.any { it.isImage } || embeds.any {
            it.image !== null || it.videoInfo !== null
        }
    }

    override val name = "Clean"
    override val aliases = arrayOf("Clear", "Prune")
    override val arguments = "[Flags]"
    override val help = "Cleans messages from a channel."
    override val botPermissions = arrayOf(Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)

    override suspend fun execute(ctx: CommandContext) {
        var args = ctx.args

        // Reason
        val reasonMatcher = reasonPattern.matchEntire(args)
        val reason: String? = if(reasonMatcher !== null) reason@ {
            val groups = reasonMatcher.groupValues
            args = groups[1]
            return@reason groups[2]
        } else null

        val quotes = HashSet<String>()
        val ids = HashSet<Long>()

        // Specific text
        quotes += quotePattern.findAll(args).map { it.groupValues[1].trim().toLowerCase() }
        args = quotePattern.replace(args, "").trim()

        // Mentions
        ctx.message.mentionedUsers.forEach { ids.add(it.idLong) }
        args = args.replace(userMention, "").trim()

        // Raw ID's
        val idsMatcher = discordID.findAll(args)
        for(res in idsMatcher)
            ids.add(res.groupValues[1].trim().toLong())
        args = args.replace(discordID, "").trim()

        // Bots Flag
        val bots = args.contains("bots", true)
        if(bots) args = args.replace("bots", "", true).trim()

        // Embeds Flag
        val embeds = args.contains("embeds", true)
        if(embeds) args = args.replace("embeds", "", true)

        // Links Flag
        val links = args.contains("links", true)
        if(links) args = args.replace("links", "", true)

        // Images Flag
        val images = args.contains("images", true)
        if(images) args = args.replace("images", "", true)

        // Files Flag
        val files = args.contains("files", true)
        if(files) args = args.replace("files", "", true)

        // Checks to clean all
        val cleanAll = quotes.isEmpty() && ids.isEmpty() && !bots && !embeds && !links && !images && !files

        // Number of messages to delete
        val numMatcher = numberPattern.findAll(args.trim())
        val num = if(numMatcher.any()) num@ {
            val n = numMatcher.first().value.trim().toInt()
            if(n < 2 || n > 1000) return ctx.invalidArgs {
                "The number of messages to delete must be between 2 and 1000!"
            } else return@num n + 1
        } else if(!cleanAll) 100 else return ctx.invalidArgs {
            "`${ctx.args}` is not a valid number of messages!"
        }

        val twoWeeksPrior = ctx.message.creationTime.minusWeeks(2).plusMinutes(1)
        val messages = LinkedList<Message>()
        val channel = ctx.textChannel
        val receiver = channel.producePast(coroutineContext, num, MaxRetrievable) breakIf@ {
            if(it.isEmpty()) return@breakIf true
            return@breakIf it.last().creationTime.isBefore(twoWeeksPrior)
        }

        while(!receiver.isClosedForReceive) {
            messages += receiver.receiveOrNull() ?: break
        }

        messages -= ctx.message // Remove call message
        var pastTwoWeeks = false

        // Get right away if we're cleaning all
        val toDelete = if(cleanAll) messages else toDelete@ {
            val toDelete = LinkedList<Message>()
            // Filter based on flags
            for(message in messages) loop@ {
                if(!message.creationTime.isBefore(twoWeeksPrior)) {
                    toDelete += when {
                        message.author.idLong in ids                             -> message
                        bots && message.author.isBot                             -> message
                        embeds && message.embeds.isNotEmpty()                    -> message
                        links && linkPattern in message.contentRaw               -> message
                        files && message.attachments.isNotEmpty()                -> message
                        images && message.hasImage                               -> message
                        quotes.any { it in message.contentRaw.toLowerCase() }    -> message
                        else -> return@loop
                    }
                } else {
                    pastTwoWeeks = true
                    break
                }
            }
            return@toDelete toDelete
        }

        // If it's empty, either nothing fit the criteria or all of it was past 2 weeks
        if(toDelete.isEmpty()) return ctx.replyError {
            if(pastTwoWeeks) "Messages older than 2 weeks cannot be deleted!"
            else "Found no messages to delete!"
        }

        val numDeleted = toDelete.size

        try {
            var i = 0
            while(i < numDeleted) { // Delet this
                if(i + MaxRetrievable > numDeleted) {
                    if(i + 1 == numDeleted) toDelete[numDeleted - 1].delete().await()
                    else channel.deleteMessages(toDelete.subList(i, numDeleted)).await()
                } else channel.deleteMessages(toDelete.subList(i, i + MaxRetrievable)).await()

                i += MaxRetrievable
            }

            ModLog.newClean(ctx.member, channel, numDeleted, reason)
            ctx.sendSuccess("Successfully cleaned $numDeleted messages!")
        } catch(t: Throwable) {
            Laxus.Log.error("An error occurred", t)
            // If something happens, we want to make sure that we inform them because
            // messages may have already been deleted.
            ctx.replyError("An error occurred when deleting $numDeleted messages!")
        }
    }
}
