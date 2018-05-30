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
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.producePast
import xyz.laxus.listeners.ModLog
import xyz.laxus.util.discordID
import xyz.laxus.util.reasonPattern
import xyz.laxus.util.userMention

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments
class CleanCommand: Command(ModeratorGroup) {
    private companion object {
        private const val MaxRetrievable = 100
        private val numberPattern = Regex("\\d{1,4}")
        private val linkPattern = Regex("https?://\\S+", RegexOption.IGNORE_CASE)
        private val quotePattern = Regex("\"(.*?)\"", RegexOption.DOT_MATCHES_ALL)

        private val Message.hasImage get() = attachments.any { it.isImage } || embeds.any {
            it.image !== null || it.videoInfo !== null
        }
    }

    override val name = "Clean"
    override val aliases = arrayOf("Clear", "Prune")
    override val arguments = "[Flags]"
    override val help = "Cleans message from a channel."
    override val botPermissions = arrayOf(Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY)

    override suspend fun execute(ctx: CommandContext) {
        var args = ctx.args

        // reason
        val reason = reasonPattern.matchEntire(args)?.let reason@ {
            val groups = it.groupValues
            args = groups[1]
            return@reason groups[2]
        }

        // quotes
        val quotes = quotePattern.findAll(args).mapTo(hashSetOf()) { it.groupValues[1].trim().toLowerCase() }
        args = args.replace(quotePattern, "").trim()

        // number of messages (kinda)
        //
        // I am legally required to explain to my future self what exactly this means
        //in order to protect myself from future harassment and self deprecation.
        // The Regex "discordID" matches 1 or more digits, and in order to avoid
        //the number of messages from being consumed by it before it's able to process.
        // In order to correct this, I have to do the actual processing in two separate
        //parts, the first part actually processes and potentially modifies the args
        //and the second part identifies if the arguments are valid.
        val numberMatcher = numberPattern.findAll(args)
        var number = if(numberMatcher.any()) {
            val n = numberMatcher.first().value.trim().toInt()
            args = args.replaceFirst(numberPattern, "")
            if(n in 2..200) n + 1 else return ctx.replyError {
                "The number of messages to delete must be between 2 and 200!"
            }
        } else 0

        // ids
        val ids = hashSetOf<Long>()

        // mention IDs
        ctx.message.mentionedUsers.mapTo(ids) { it.idLong }
        args = args.replace(userMention, "").trim()

        // raw IDs
        discordID.findAll(args).mapTo(ids) { it.groupValues[0].trim().toLong() }
        args = args.replace(discordID, "").trim()

        // bots
        val bots = args.contains("bots", true)
        if(bots) args = args.replace("bots", "", true).trim()

        // embeds
        val embeds = args.contains("embeds", true)
        if(embeds) args = args.replace("embeds", "", true).trim()

        // links
        val links = args.contains("links", true)
        if(links) args = args.replace("links", "", true).trim()

        // images
        val images = args.contains("images", true)
        if(images) args = args.replace("images", "", true).trim()

        // files
        val files = args.contains("files", true)
        if(files) args = args.replace("files", "", true).trim()

        // clean all?
        val cleanAll = quotes.isEmpty() && ids.isEmpty() && !bots && !embeds && !links && !images && !files

        // number of messages (again)
        number = if(number > 0) number else if(!cleanAll) 100 else return ctx.invalidArgs {
            "`${ctx.args}` is not a valid number of messages!"
        }

        val twoWeeksPrior = ctx.message.creationTime.minusWeeks(2).plusMinutes(1)
        val channel = ctx.textChannel
        val receiver = channel.producePast(ctx, number = number, retrieveLimit = MaxRetrievable) receiver@ {
            // break if it is empty
            if(it.isEmpty()) return@receiver true
            // break if the pass retrieved any messages
            //whose creation time is before two weeks prior
            return@receiver it.any { it.creationTime.isBefore(twoWeeksPrior) }
        }

        val messages = hashSetOf<Message>()

        // while we are still receiving from the production
        while(!receiver.isClosedForReceive) {
            messages += receiver.receiveOrNull() ?: break
        }

        messages -= ctx.message // Remove call message
        var pastTwoWeeks = false
        val toDelete = arrayListOf<Message>()
        for(message in messages) {
            if(!message.creationTime.isBefore(twoWeeksPrior)) {
                toDelete += if(cleanAll) message else when {
                    message.author.idLong in ids                          -> message
                    bots && message.author.isBot                          -> message
                    embeds && message.embeds.isNotEmpty()                 -> message
                    links && linkPattern in message.contentRaw            -> message
                    files && message.attachments.isNotEmpty()             -> message
                    images && message.hasImage                            -> message
                    quotes.any { it in message.contentRaw.toLowerCase() } -> message
                    else -> null
                } ?: continue
            } else {
                pastTwoWeeks = true
                break
            }
        }

        // If it's empty, either nothing fit the criteria or all of it was past 2 weeks
        if(toDelete.isEmpty()) return ctx.replyError("Found no messages to delete!")
        if(pastTwoWeeks) return ctx.replyError {
            "Messages older than 2 weeks cannot be deleted!"
        }

        val numberToDelete = toDelete.size
        for(i in 0 until numberToDelete step MaxRetrievable) {
            val nextStep = i + MaxRetrievable
            if(nextStep > numberToDelete) {
                when(i + 1) {
                    numberToDelete -> toDelete[numberToDelete - 1].delete().await()
                    else -> channel.deleteMessages(toDelete.subList(i, numberToDelete)).await()
                }
            } else {
                channel.deleteMessages(toDelete.subList(i, nextStep)).await()
            }
        }

        ModLog.newClean(ctx.member, channel, numberToDelete, reason)
        ctx.replySuccess("Successfully cleaned $numberToDelete messages!")
    }
}
