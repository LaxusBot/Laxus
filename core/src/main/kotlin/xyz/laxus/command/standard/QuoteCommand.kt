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

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
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
class QuoteCommand : Command(StandardGroup) {
    override val name = "Quote"
    override val arguments = "<Channel ID> [Message ID]"
    override val help = "Quotes a message by ID."
    override val cooldown = 10
    override val cooldownScope = CooldownScope.USER
    override val guildOnly = true
    override val botPermissions = arrayOf(Permission.MESSAGE_EMBED_LINKS)

    override suspend fun execute(ctx: CommandContext) {
        val split = ctx.args.split(commandArgs)
        val channel: TextChannel
        val message: Message

        when(split.size) {
            1 -> {
                channel = ctx.textChannel
                message = try {
                    channel.getMessageById(split[0].toLong()).await()
                } catch(e: NumberFormatException) {
                    return ctx.invalidArgs { "\"${split[0]}\" is not a valid message ID" }
                } catch(e : Exception) {
                    return ctx.replyError("Could not find a message with ID: ${split[0]}!")
                }
            }

            2 -> {
                channel = try {
                    ctx.guild.getTextChannelById(split[0].toLong()) ?: return ctx.replyError {
                        "Could not find a channel with ID: ${split[0]}!"
                    }
                } catch (e: NumberFormatException) {
                    return ctx.invalidArgs { "\"${split[0]}\" is not a valid channel ID" }
                }

                message = try {
                    channel.getMessageById(split[1].toLong()).await()
                } catch (e: NumberFormatException) {
                    return ctx.invalidArgs { "\"${split[1]}\" is not a valid message ID" }
                } catch (e : Exception) {
                    return ctx.replyError("Could not find a message with ID: ${split[1]}!")
                }
            }

            else -> return ctx.replyError {
                "**Too Many Arguments!**\n" +
                "Provide either a channel ID and message ID or just a message ID!"
            }
        }

        ctx.reply(embed {
            author {
                value = message.author.formattedName(false)
                url = message.author.effectiveAvatarUrl
                icon = message.author.effectiveAvatarUrl
            }

            + message.contentRaw

            time { message.creationTime }
            color { message.member?.color }
        })

        ctx.invokeCooldown()
    }
}
