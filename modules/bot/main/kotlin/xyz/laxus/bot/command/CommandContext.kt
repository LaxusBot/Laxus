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

package xyz.laxus.bot.command

import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.RestAction
import xyz.laxus.bot.Bot
import xyz.laxus.bot.Laxus
import xyz.laxus.bot.utils.emoteRegex
import xyz.laxus.bot.utils.jda.await
import xyz.laxus.bot.utils.jda.filterMassMentions

class CommandContext internal constructor(
    val event: MessageReceivedEvent,
    args: String,
    val bot: Bot
) {
    val jda: JDA get() = event.jda
    val author: User get() = event.author
    val message: Message get() = event.message
    val channel: MessageChannel get() = event.channel
    val channelType: ChannelType get() = channel.type
    val selfUser: SelfUser get() = jda.selfUser
    val messageIdLong: Long get() = event.messageIdLong

    val guild: Guild get() = checkGuild(event.guild)
    val member: Member get() = checkGuild(event.member)
    val textChannel: TextChannel get() = checkGuild(event.textChannel)
    val selfMember: Member get() = guild.selfMember

    val privateChannel: PrivateChannel get() = checkPrivate(event.privateChannel)

    val args get() = _args

    val isDev get() = author.idLong == Laxus.DevId
    val isGuild get() = channelType.isGuild
    val isPrivate get() = !isGuild

    private var _args = args

    internal fun linkMessage(message: Message) = bot.linkCall(messageIdLong, message)

    fun reply(text: String) {
        checkForTalking(channel)
        sendMessage(text, channel).queue(this::linkMessage)
    }

    fun reply(embed: MessageEmbed) {
        checkForTalking(channel)
        channel.sendMessage(embed).queue(this::linkMessage)
    }

    fun reply(message: Message) {
        checkForTalking(channel)
        channel.sendMessage(message).queue(this::linkMessage)
    }

    suspend fun send(text: String): Message {
        checkForTalking(channel)
        return sendMessage(text, channel).await().also(this::linkMessage)
    }

    suspend fun send(embed: MessageEmbed): Message {
        checkForTalking(channel)
        return channel.sendMessage(embed).await().also(this::linkMessage)
    }

    suspend fun send(message: Message): Message {
        checkForTalking(channel)
        return channel.sendMessage(message).await().also(this::linkMessage)
    }

    fun replyInDM(text: String) {
        if(channel is PrivateChannel) {
            return sendMessage(text, channel).queue()
        }
        author.openPrivateChannel().queue({ pc ->
            sendMessage(text, pc).queue()
        }, {
            if(isGuild && textChannel.canTalk()) {
                replyError(BlockingError)
            }
        })
    }

    fun replyInDM(embed: MessageEmbed) {
        if(channel is PrivateChannel) {
            return channel.sendMessage(embed).queue()
        }
        author.openPrivateChannel().queue({ pc ->
            pc.sendMessage(embed).queue()
        }, {
            if(isGuild && textChannel.canTalk()) {
                replyError(BlockingError)
            }
        })
    }

    fun replyInDM(message: Message) {
        if(channel is PrivateChannel) {
            return channel.sendMessage(message).queue()
        }
        author.openPrivateChannel().queue({ pc ->
            pc.sendMessage(message).queue()
        }, {
            if(isGuild && textChannel.canTalk()) {
                replyError(BlockingError)
            }
        })
    }

    fun replySuccess(text: String) = reply("${Laxus.Success} $text")
    fun replyWarning(text: String) = reply("${Laxus.Warning} $text")
    fun replyError(text: String) = reply("${Laxus.Error} $text")

    fun reactSuccess() = react(Laxus.Success)
    fun reactWarning() = react(Laxus.Warning)
    fun reactError() = react(Laxus.Error)

    inline fun reply(block: () -> String) = reply(block())
    inline fun replySuccess(block: () -> String) = replySuccess(block())
    inline fun replyWarning(block: () -> String) = replyWarning(block())
    inline fun replyError(block: () -> String) = replyError(block())

    suspend fun sendSuccess(text: String) = send("${Laxus.Success} $text")
    suspend fun sendWarning(text: String) = send("${Laxus.Warning} $text")
    suspend fun sendError(text: String) = send("${Laxus.Error} $text")

    suspend inline fun send(block: () -> String) = send(block())
    suspend inline fun sendSuccess(block: () -> String) = sendSuccess(block())
    suspend inline fun sendWarning(block: () -> String) = sendWarning(block())
    suspend inline fun sendError(block: () -> String) = sendError(block())

    inline fun error(type: String, details: () -> String): Nothing = commandError("**$type!**\n${details()}")

    internal fun reassignArgs(args: String) {
        _args = args
    }

    private fun react(string: String) {
        if(emoteRegex matches string) {
            jda.getEmoteById(emoteRegex.replace(string, "$1"))?.let(::addReaction)
        } else {
            addReaction(string)
        }
    }

    private fun addReaction(emote: String) {
        if(isGuild) {
            if(selfMember.hasPermission(textChannel, Permission.MESSAGE_ADD_REACTION)) {
                message.addReaction(emote).queue({},{})
            }
        } else {
            message.addReaction(emote).queue({},{})
        }
    }

    private fun addReaction(emote: Emote) {
        if(event.isFromType(ChannelType.TEXT)) {
            if(selfMember.hasPermission(textChannel, Permission.MESSAGE_ADD_REACTION)) {
                message.addReaction(emote).queue({},{})
            }
        } else {
            message.addReaction(emote).queue({},{})
        }
    }

    private fun sendMessage(text: String, channel: MessageChannel): RestAction<Message> {
        val parts = processMessage(text)
        if(parts.size == 1) {
            return channel.sendMessage(parts[0])
        }

        parts.forEachIndexed { i, s ->
            val action = channel.sendMessage(s)
            if(i == parts.size - 1 || i == MaxMessages - 1)
                return action
            else action.queue(::linkMessage, {})
        }

        throw IllegalStateException("Somehow iterated through all message parts " +
                                    "without returning final RestAction?!")
    }

    companion object {
        const val MaxMessages = 2
        const val BlockingError = "Help could not be sent to your DM because you are blocking me."

        fun processMessage(input: String): List<String> {
            val msgs = arrayListOf<String>()
            var toSend = filterMassMentions(input)
            while(toSend.length > 2000) {
                val leeway = 2000 - (toSend.length % 2000)
                var index = toSend.lastIndexOf("\n", 2000)
                if(index < leeway)
                    index = toSend.lastIndexOf(" ", 2000)
                if(index < leeway)
                    index = 2000
                val temp = toSend.substring(0, index).trim()
                if(temp.isNotEmpty())
                    msgs.add(temp)
                toSend = toSend.substring(index).trim()
            }
            if(toSend.isNotEmpty())
                msgs.add(toSend)
            return msgs
        }

        private fun checkForTalking(channel: MessageChannel) {
            check(channel !is TextChannel || channel.canTalk()) {
                "Cannot send a message to a TextChannel without being able to talk in it!"
            }
        }

        private fun <T: Any> CommandContext.checkGuild(entity: T?): T {
            check(isGuild) { "Context is not from a guild!" }
            return checkNotNull(entity) { "Guild-only entity was null even when context was from guild!" }
        }

        private fun <T: Any> CommandContext.checkPrivate(entity: T?): T {
            check(isPrivate) { "Context is not from a private channel!" }
            return checkNotNull(entity) { "Private-only entity was null even when context was from private channel!" }
        }
    }
}
