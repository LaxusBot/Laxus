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
package xyz.laxus.command.standard

import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.exceptions.ErrorResponseException
import net.dv8tion.jda.core.requests.ErrorResponse.*
import xyz.laxus.command.Command
import xyz.laxus.command.CommandContext
import xyz.laxus.command.MustHaveArguments
import xyz.laxus.jda.util.await
import xyz.laxus.jda.util.embed
import xyz.laxus.util.commandArgs
import xyz.laxus.util.formattedName

/**
 * @author Kaidan Gustave
 */
@MustHaveArguments("Please specify a message ID to quote!")
class QuoteCommand: Command(StandardGroup) {
    private companion object {
        private const val CannotFindMessage = "Could not find a message with ID: %s!"
        private const val InvalidID = "\"%s\" is not a valid %s ID"
    }

    override val name = "Quote"
    override val arguments = "<Channel ID> [Message ID]"
    override val help = "Quotes a message by ID."
    override val cooldown = 10
    override val cooldownScope = CooldownScope.USER
    override val guildOnly = true
    override val botPermissions = arrayOf(MESSAGE_EMBED_LINKS, MESSAGE_HISTORY)

    override suspend fun execute(ctx: CommandContext) {
        // Split into parts
        val split = ctx.args.split(commandArgs)

        val channel: TextChannel
        val message: Message

        // When...
        when(split.size) {
            // Only one argument
            1 -> {
                // Channel is the calling channel
                channel = ctx.textChannel

                // Check for necessary permission
                if(!ctx.selfMember.hasPermission(channel, MESSAGE_HISTORY)) return ctx.replyError {
                    // Note that because the command is running, this means we must have
                    //MESSAGE_WRITE permissions, and thus we must also have implied
                    //MESSAGE_READ permissions, so we only need to check for MESSAGE_HISTORY
                    //as a result.
                    "I cannot use the `$name` command in ${channel.asMention} because I do not have permission!"
                }

                message = try {
                    channel.getMessageById(split[0].toLong()).await()
                } catch(e: NumberFormatException) {
                    return ctx.invalidArgs { InvalidID.format(split[0], "message") }
                } catch(e: ErrorResponseException) {
                    if(e.errorResponse == UNKNOWN_MESSAGE) return ctx.replyError(CannotFindMessage.format(split[0]))
                    throw e
                }
            }

            2 -> {
                channel = try {
                    ctx.guild.getTextChannelById(split[0].toLong()) ?: return ctx.replyError {
                        "Could not find a channel with ID: ${split[0]}!"
                    }
                } catch(e: NumberFormatException) {
                    return ctx.invalidArgs { InvalidID.format(split[0], "channel") }
                }

                // Check for necessary permission
                if(!ctx.selfMember.hasPermission(channel, MESSAGE_HISTORY, MESSAGE_READ)) return ctx.replyError {
                    // Unlike the single ID case, it's still good practice to check
                    //if we have MESSAGE_READ before continuing.
                    "I cannot use the `$name` command in ${channel.asMention} because I do not have permission!"
                }

                message = try {
                    channel.getMessageById(split[1].toLong()).await()
                } catch(e: NumberFormatException) {
                    return ctx.invalidArgs { InvalidID.format(split[1], "message") }
                } catch(e: ErrorResponseException) {
                    if(e.errorResponse == UNKNOWN_MESSAGE) return ctx.replyError(CannotFindMessage.format(split[1]))
                    throw e
                }
            }

            else -> return ctx.error("Too Many Arguments") {
                "Provide either a channel ID and message ID or just a message ID!"
            }
        }

        ctx.reply(embed {
            author {
                this.value = message.author.formattedName(false)
                this.url = message.author.effectiveAvatarUrl
                this.icon = message.author.effectiveAvatarUrl
            }

            + message.contentRaw

            time { message.creationTime }
            color { message.member?.color }
        })

        ctx.invokeCooldown()
    }
}
